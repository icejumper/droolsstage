/*
 * [y] hybris Platform
 *
 * Copyright (c) 2017 SAP SE or an SAP affiliate company.  All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.ruleengine.stage;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.kie.internal.command.CommandFactory.newInsertElements;

import de.hybris.ruleengine.stage.init.KieContainerListener;
import de.hybris.ruleengine.stage.init.RuleEngineContainerRegistry;
import de.hybris.ruleengine.stage.init.RuleEngineKieModuleSwapper;
import de.hybris.ruleengine.stage.init.RuleEvaluationContext;
import de.hybris.ruleengine.stage.init.StatelessKieSessionHelper;
import de.hybris.ruleengine.stage.init.impl.DefaultRuleEngineBootstrap;
import de.hybris.ruleengine.stage.model.RulesModule;
import de.hybris.ruleengine.stage.utils.MultiFlag;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.drools.core.command.runtime.rule.FireAllRulesCommand;
import org.kie.api.builder.ReleaseId;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.api.runtime.rule.AgendaFilter;
import org.kie.internal.command.CommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;


@Service
public class RuleEngineStageService
{
	private int evaluationRetryLimit = 2;
	public static final String MODULE_MVN_VERSION_NONE = "NONE";

	@Autowired
	private RuleEngineKieModuleSwapper ruleEngineKieModuleSwapper;
	@Autowired
	private RuleEngineContainerRegistry<ReleaseId, KieContainer> ruleEngineContainerRegistry;
	@Autowired
	private StatelessKieSessionHelper statelessKieSessionHelper;
	@Autowired
	private DefaultRuleEngineBootstrap ruleEngineBootstrap;

	private MultiFlag initialisationMultiFlag = new MultiFlag();

	private Map<ReleaseId, KieContainer> kieContainers = Maps.newConcurrentMap();

	private static Logger LOGGER = LoggerFactory.getLogger(RuleEngineStageService.class);

	public RuleEvaluationResult evaluate(final RuleEvaluationContext context)
	{
		if (LOGGER.isDebugEnabled() && nonNull(context.getFacts()))
		{
			LOGGER.debug("Rule evaluation triggered with the facts: {}", context.getFacts().toString());
		}

		try
		{
			final RuleEvaluationResult result = new RuleEvaluationResult();
			final ReleaseId deployedReleaseId = statelessKieSessionHelper.getDeployedKieModuleReleaseId(context);
			KieContainer kContainer = ruleEngineContainerRegistry.getActiveContainer(deployedReleaseId);
			if (isNull(kContainer))
			{
				LOGGER.info("KieContainer with releaseId [{}] was not found. Trying to look up for closest matching version...",
						deployedReleaseId);

				if(!kieContainers.isEmpty())
				{
					final ReleaseId tryDeployedReleaseId = kieContainers.keySet().iterator().next();

					LOGGER.info("Found KieContainer with releaseId [{}]", tryDeployedReleaseId);
					kContainer = ruleEngineContainerRegistry.getActiveContainer(tryDeployedReleaseId);
				}
				else
				{
				  throw new RuntimeException(
						  "Cannot complete the evaluation: rule engine was not initialized for releaseId ["
								  + deployedReleaseId
								  + "]");
				}
			}

			final AgendaFilter agendaFilter = (AgendaFilter) context.getFilter();

			final List<Command> commands = newArrayList();
			commands.add(newInsertElements(context.getFacts()));
			commands.add(nonNull(agendaFilter) ? new FireAllRulesCommand(agendaFilter) : new FireAllRulesCommand());
			final BatchExecutionCommand command = CommandFactory.newBatchExecution(commands);

			final StatelessKieSession session = statelessKieSessionHelper.initializeSession(context, kContainer);
			// execute drools command
			LOGGER.debug("Executing StatelessKieSession.execute for releaseId [{}]", kContainer.getReleaseId());
			final ExecutionResults executionResults = tryExecution(session, command, 1);
			result.setExecutionResult(executionResults);
			return result;
		}
		finally
		{
			// make sure to release the lock again
			ruleEngineContainerRegistry.unlockReadingRegistry();
		}
	}

	protected ExecutionResults tryExecution(final StatelessKieSession session, final BatchExecutionCommand command, // NOSONAR
			final int currentAttempt)
	{
		try
		{
			// execute drools command
			return session.execute(command);
		}
		catch (final RuntimeException re)
		{
			if (re.getMessage() != null && re.getMessage().startsWith("Unable to find query '"))
			{
				LOGGER.warn("rule evaluation failed due to missing query. Attempt: {}: {}", currentAttempt, re.getMessage());
				// query error, lets retry if still below limit
				if (currentAttempt <= evaluationRetryLimit)
				{
					return tryExecution(session, command, currentAttempt + 1);
				}
				LOGGER.error("error during rule evaluation due to missing query. Giving up after {} attempts.", currentAttempt);
			}
			throw re;
		}
	}

	private void initializeNonBlocking(final RulesModule moduleModel,
			final String deployedMvnVersion,
			final RuleEngineActionResult result)
	{

		final Optional<ReleaseId> oldDeployedReleaseId = ruleEngineKieModuleSwapper.getDeployedReleaseId(moduleModel, deployedMvnVersion);
		final String oldVersion = oldDeployedReleaseId.map(ReleaseId::getVersion).orElse(MODULE_MVN_VERSION_NONE);
		switchKieModule(moduleModel, new KieContainerListener()
		{
			@Override
			public void onSuccess(final KieContainer kieContainer)
			{
				doSwapKieContainers(kieContainer, result, moduleModel, deployedMvnVersion);
			}

			@Override
			public void onFailure(final RuleEngineActionResult result)
			{
				result.setDeployedVersion(oldVersion);
			}
		}, result, newArrayList(() -> oldDeployedReleaseId
				.map(oldReleaseId -> removeOldKieModuleIfSwapSuccessful(result, oldReleaseId)).orElse(false)));

	}

	protected boolean removeOldKieModuleIfSwapSuccessful(final RuleEngineActionResult result, final ReleaseId oldReleaseId)
	{
		boolean removed = false;
		if (!result.isActionFailed())
		{
			removed = ruleEngineKieModuleSwapper.removeKieModuleIfPresent(oldReleaseId, result);
		}
		return removed;
	}

	protected void doSwapKieContainers(final KieContainer kieContainer, 
			final RuleEngineActionResult ruleEngineActionResult, final RulesModule module,
			final String deployedReleaseIdVersion)
	{
		ruleEngineContainerRegistry.lockWritingRegistry();
		try
		{
			ruleEngineBootstrap.activateNewRuleEngineContainer(kieContainer, ruleEngineActionResult, module,
					deployedReleaseIdVersion);
		}
		finally
		{
			ruleEngineContainerRegistry.unlockWritingRegistry();
		}
		LOGGER.info("Swapping to a newly created container [{}] is finished successfully", kieContainer.getReleaseId());
	}

	protected void switchKieModule(final RulesModule module,
			final KieContainerListener listener,
			final RuleEngineActionResult result, final Collection<Supplier<Object>> chainOfPostTasks)
	{
		final String moduleName = module.getName();
		if (initialisationMultiFlag.compareAndSet(moduleName, false, true))
		{
			final Supplier<Object> resetFlagSupplier = () -> initialisationMultiFlag.compareAndSet(moduleName, true, false);
			try
			{
				final List<Object> resultAccumulator = newArrayList();
				final LinkedList<Supplier<Object>> postTaskList = newLinkedList();
				if (nonNull(chainOfPostTasks))
				{
					postTaskList.addAll(chainOfPostTasks);
				}
				postTaskList.addLast(resetFlagSupplier);
				ruleEngineKieModuleSwapper.switchKieModuleAsync(moduleName, listener, resultAccumulator, resetFlagSupplier,
						postTaskList, result);
			}
			catch (final Exception e) //NOSONAR
			{
				resetFlagSupplier.get();
				listener.onFailure(result);
			}
		}
		else
		{
			LOGGER.error("Kie containers swapping is in progress, no rules updates are possible at this time");
			throw new RuntimeException(
					"Kie containers swapping is in progress, no rules updates are possible at this time");
		}
	}

	public RuleEngineStageService initialize(final RulesModule abstractModule, final String deployedMvnVersion, final RuleEngineActionResult result)
	{
		initializeNonBlocking(abstractModule, deployedMvnVersion, result);
		ruleEngineKieModuleSwapper.waitForSwappingToFinish();

		return this;
	}

}
