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
package de.hybris.ruleengine.stage.init.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.springframework.util.CollectionUtils.isEmpty;

import de.hybris.ruleengine.stage.RuleEngineActionResult;
import de.hybris.ruleengine.stage.RulesModuleRepo;
import de.hybris.ruleengine.stage.concurrency.RulePublishingFuture;
import de.hybris.ruleengine.stage.concurrency.RulePublishingSpliterator;
import de.hybris.ruleengine.stage.concurrency.TaskResult;
import de.hybris.ruleengine.stage.init.KieContainerListener;
import de.hybris.ruleengine.stage.init.RuleDeploymentTaskResult;
import de.hybris.ruleengine.stage.init.RuleEngineBootstrap;
import de.hybris.ruleengine.stage.init.RuleEngineKieModuleSwapper;
import de.hybris.ruleengine.stage.model.DroolsKieSession;
import de.hybris.ruleengine.stage.model.Rule;
import de.hybris.ruleengine.stage.model.RulesBase;
import de.hybris.ruleengine.stage.model.RulesModule;
import de.hybris.ruleengine.stage.utils.ModuleVersionUtils;
import de.hybris.ruleengine.stage.utils.RuleEngineUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.BooleanUtils;
import org.drools.compiler.compiler.io.memory.MemoryFileSystem;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kie.builder.impl.MemoryKieModule;
import org.drools.compiler.kproject.ReleaseIdImpl;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.builder.model.KieSessionModel.KieSessionType;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;



/**
 * Default (drools-based) implementation of the {@link RuleEngineKieModuleSwapper} interface
 */
@Component
public class DefaultRuleEngineKieModuleSwapper implements RuleEngineKieModuleSwapper
{

	private static final String BASE_WORKER_NAME = "RuleEngine-module-swapping";

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRuleEngineKieModuleSwapper.class);

	private long workerPreDestroyTimeout;
	private KieServices kieServices;
	@Autowired
	private RulePublishingSpliterator rulePublishingSpliterator;
	@Autowired
	private RuleEngineBootstrap<KieServices, KieContainer> ruleEngineBootstrap;
	@Autowired
	private ModuleVersionUtils moduleVersionUtils;

	private ThreadFactory threadFactory;

	private Map<String, Set<Thread>> asyncWorkers;

	@Override
	public List<Object> switchKieModule(final RulesModule module,
			final KieContainerListener listener, final LinkedList<Supplier<Object>> postTaskList,        // NOSONAR
			final RuleEngineActionResult result)
	{
		final List<Object> resultsAccumulator = newArrayList();
		try
		{
			initializeNewModule(module, listener, result);
		}
		finally
		{
			postTaskList.forEach(pt -> resultsAccumulator.add(pt.get()));
		}
		return resultsAccumulator;
	}


	@Override
	public void switchKieModuleAsync(final String moduleName,
			final KieContainerListener listener, final List<Object> resultsAccumulator, final Supplier<Object> resetFlagSupplier,
			final List<Supplier<Object>> postTaskList, final RuleEngineActionResult result)
	{
		waitForSwappingToFinish(moduleName);
		LOGGER.info("swapping started for rule module : {}", moduleName);
		final Thread asyncWorker = threadFactory.newThread(switchKieModuleRunnableTask(moduleName,
				listener, resultsAccumulator, resetFlagSupplier, postTaskList, result));
		asyncWorker.setName(getNextWorkerName());
		asyncWorker.start();
		registerWorker(moduleName, asyncWorker);
	}

	@Override
	public void waitForSwappingToFinish()
	{
		asyncWorkers.entrySet().stream().flatMap(e -> e.getValue().stream()).forEach(this::waitWhileWorkerIsRunning);
	}

	protected void waitForSwappingToFinish(final String moduleName)
	{
		asyncWorkers.entrySet().stream().filter(e -> e.getKey().equals(moduleName)).flatMap(e -> e.getValue().stream())
				.forEach(this::waitWhileWorkerIsRunning);
	}

	protected String getNextWorkerName()
	{
		long nextActiveOrder = 0;
		if (!isEmpty(asyncWorkers))
		{
			nextActiveOrder = asyncWorkers.entrySet().stream().flatMap(e -> e.getValue().stream())
					.filter(w -> nonNull(w) && w.isAlive()).count();
		}
		return BASE_WORKER_NAME + "-" + nextActiveOrder;
	}

	protected void waitWhileWorkerIsRunning(final Thread worker)
	{
		if (nonNull(worker) && worker.isAlive())
		{
			try
			{
				LOGGER.info("Waiting for a currently running async worker to finish the job...");
				worker.join(3600000L);
			}
			catch (final InterruptedException e)
			{
				Thread.currentThread().interrupt();
				LOGGER.debug("Interrupted exception is caught during async Kie container swap: {}", e);
			}
		}
	}

	/**
	 * This method to be called by containers (like spring container) as destroy method
	 */
	public void beforeDestroy()
	{
		waitForSwappingToFinish();
	}

	@Override
	public void writeKModuleXML(final KieModuleModel module, final KieFileSystem kfs)
	{
		kfs.writeKModuleXML(module.toXML());
	}

	@Override
	public void writePomXML(final RulesModule module, final KieFileSystem kfs)
	{
		final ReleaseId releaseId = getReleaseId(module);
		LOGGER.debug("Writing POM for releaseId: {}", releaseId.toExternalForm());
		kfs.generateAndWritePomXML(releaseId);
	}

	@Override
	public ReleaseId getReleaseId(final RulesModule module)
	{
		final String moduleVersion = moduleVersionUtils.getDeployedRulesModuleVersion(module, false);
		return getKieServices().newReleaseId(module.getMvnGroupId(), module.getMvnArtifactId(), moduleVersion);
	}

	protected ReleaseId getNextReleaseId(final RulesModule module)
	{
		final String moduleVersion = moduleVersionUtils.getDeployedRulesModuleVersion(module, true);
		return getKieServices().newReleaseId(module.getMvnGroupId(), module.getMvnArtifactId(), moduleVersion);
	}

	@Override
	public KieModule createKieModule(final RulesModule module,
			final RuleEngineActionResult result)
	{
		final Collection<RulesBase> kieBases = module.getKieBases();
		Preconditions.checkArgument(org.apache.commons.collections.CollectionUtils.isNotEmpty(kieBases),
				"kieBases in the module must not be null");

		final KieModuleModel kieModuleModel = getKieServices().newKieModuleModel();
		kieBases.forEach(base -> addKieBase(kieModuleModel, base));

		final ReleaseId newReleaseId = getNextReleaseId(module);
		final List<KieBuilder> kieBuilders = kieBases.stream()
				.flatMap(base -> deployRules(module, kieModuleModel, base).stream()).collect(toList());

		return mergePartialKieModules(newReleaseId, kieModuleModel, kieBuilders);
	}

	protected void mergeFileSystemToKieModule(final MemoryKieModule partialKieModule, final MemoryFileSystem mainMemoryFileSystem)
	{
		final MemoryFileSystem partialMemoryFileSystem = partialKieModule.getMemoryFileSystem();
		final Map<String, byte[]> fileContents = partialMemoryFileSystem.getMap();
		for (final Map.Entry<String, byte[]> entry : fileContents.entrySet())
		{
			mainMemoryFileSystem.write(entry.getKey(), entry.getValue());
		}
	}

	protected KieModule mergePartialKieModules(final ReleaseId releaseId, final KieModuleModel kieModuleModel,
			final List<KieBuilder> kieBuilders)
	{
		final MemoryFileSystem mainMemoryFileSystem = new MemoryFileSystem();
		final InternalKieModule returnKieModule = new MemoryKieModule(releaseId, kieModuleModel, mainMemoryFileSystem);
		if (isNotEmpty(kieBuilders))
		{
			for (final KieBuilder kieBuilder : kieBuilders)
			{
				final KieModule partialKieModule = kieBuilder.getKieModule();
				mergeFileSystemToKieModule((MemoryKieModule) partialKieModule, mainMemoryFileSystem);
			}
		}
		mainMemoryFileSystem.mark();
		LOGGER.debug("Main KIE module contains [{}] files", mainMemoryFileSystem.getFileNames().size());
		return returnKieModule;
	}

	@Override
	public void addKieBase(final KieModuleModel module, final KieFileSystem kfs, final RulesBase base)
	{
		addKieBase(module, base);
		addRules(kfs, base);
	}

	@Override
	public void addKieBase(final KieModuleModel module, final RulesBase base)
	{
		final KieBaseModel kieBaseModel = module.newKieBaseModel(base.getName());
		kieBaseModel.setEqualsBehavior(RuleEngineKieModuleSwapper.getEqualityBehaviorOption(base.getEqualityBehavior()));
		kieBaseModel.setEventProcessingMode(RuleEngineKieModuleSwapper.getEventProcessingOption(base.getEventProcessingMode()));
		base.getKieSessions().forEach(session -> addKieSession(kieBaseModel, session));
	}

	@Override
	public void addKieSession(final KieBaseModel base, final DroolsKieSession session)
	{
		final KieSessionModel kieSession = base.newKieSessionModel(session.getName());
		final KieSessionType sessionType = RuleEngineKieModuleSwapper.getSessionType(session.getSessionType());
		kieSession.setType(sessionType);
	}

	@Override
	public String activateKieModule(final RulesModule module)
	{
		final String releaseIdVersion = getReleaseId(module).getVersion();
		module.setDeployedMvnVersion(releaseIdVersion);
		return releaseIdVersion;
	}

	@Override
	public boolean removeKieModuleIfPresent(final ReleaseId releaseId, final RuleEngineActionResult result)
	{
		boolean moduleRemoved = false;
		final KieModule kieModule = getKieServices().getRepository().getKieModule(releaseId);
		if (nonNull(kieModule) && !isInitialEngineStartup(releaseId, result.getDeployedVersion()))
		{
			LOGGER.info("Removing old Kie module [{}]", releaseId);
			getKieServices().getRepository().removeKieModule(releaseId);
			moduleRemoved = true;
		}
		return moduleRemoved;
	}

	@Override
	public boolean removeOldKieModuleIfPresent(final RuleEngineActionResult result)
	{
		boolean moduleRemoved = false;
		final RulesModule rulesModule = RulesModuleRepo.findByName(result.getModuleName());
		if (nonNull(rulesModule))
		{
			final ReleaseId releaseId = new ReleaseIdImpl(rulesModule.getMvnGroupId(), rulesModule.getMvnArtifactId(),
					result.getOldVersion());
			moduleRemoved = removeKieModuleIfPresent(releaseId, result);
		}
		return moduleRemoved;
	}

	@Override
	public void addRules(final KieFileSystem kfs, final RulesBase base)
	{
		LOGGER.debug("Drools Engine Service addRules triggered...");
		final Set<Rule> rules = base.getRules();
		writeRulesToKieFileSystem(kfs, rules);
	}

	private boolean isRuleValid(final Rule rule)
	{
		return nonNull(rule.getRuleContent()) && BooleanUtils.isTrue(rule.getActive());
	}

	private void writeRulesToKieFileSystem(final KieFileSystem kfs, final Collection<Rule> rules)
	{
		for (final Rule rule : rules)
		{
			if (isRuleValid(rule))
			{
				final String rulePath = RuleEngineUtils.getRulePath(rule);
				final String drl = rule.getRuleContent();

				LOGGER.debug("{} {}", rule.getCode(), rulePath);
				LOGGER.debug("{}", drl);

				kfs.write(rulePath, drl);
			}
			if (isNull(rule.getRuleContent()))
			{
				LOGGER.warn("ignoring rule {}. No ruleContent set!", rule.getCode());
			}
		}
	}

	private String[] getRulePaths(final Collection<Rule> rules)
	{
		return rules.stream().filter(this::isRuleValid).map(RuleEngineUtils::getRulePath).collect(toList())
				.toArray(new String[] {});
	}

	@Override
	public Optional<ReleaseId> getDeployedReleaseId(final RulesModule module, final String deployedMvnVersion)
	{
		String deployedReleaseIdVersion = deployedMvnVersion;
		RulesModule localModule = module;
		if (isNull(deployedReleaseIdVersion))
		{
			localModule = RulesModuleRepo.findByName(module.getName());
			if (nonNull(localModule))
			{
				deployedReleaseIdVersion = localModule.getDeployedMvnVersion();
			}
		}
		Optional<ReleaseId> deployedReleaseId = Optional.empty();
		if (nonNull(getKieServices()) && nonNull(deployedReleaseIdVersion))
		{
			deployedReleaseId = Optional
					.of(getKieServices()
							.newReleaseId(localModule.getMvnGroupId(), localModule.getMvnArtifactId(), deployedReleaseIdVersion));
		}
		return deployedReleaseId;
	}

	@Override
	public void setUpKieServices()
	{
		if (isNull(getKieServices()))
		{
			this.kieServices = ruleEngineBootstrap.getEngineServices();
		}
	}

	@PostConstruct
	public void setUp()
	{
		threadFactory = new ThreadFactoryBuilder().build();
		workerPreDestroyTimeout = 3600000L;
		asyncWorkers = Maps.newConcurrentMap();
		setUpKieServices();
	}

	private List<KieBuilder> deployRules(final RulesModule module, final KieModuleModel kieModuleModel,
			final RulesBase kieBase)
	{
		final List<String> rulesUuids = kieBase.getRules().stream().map(Rule::getUuid).collect(toList());
		final RulePublishingFuture rulePublishingFuture = rulePublishingSpliterator
				.publishRulesAsync(kieModuleModel, getReleaseId(module), rulesUuids);
		final RuleDeploymentTaskResult ruleDeploymentResult = (RuleDeploymentTaskResult) rulePublishingFuture.getTaskResult();
		if (ruleDeploymentResult.getState().equals(TaskResult.State.FAILURE))
		{
			throw new RuntimeException("Initialization of rule engine failed during the deployment phase");
		}
		return rulePublishingFuture.getPartialKieBuilders();
	}


	@Override
	public KieContainer initializeNewKieContainer(final RulesModule module, final KieModule kieModule,
			final RuleEngineActionResult result)
	{
		final ReleaseId releaseId = getReleaseId(module);
		result.setModuleName(module.getName());
		final KieRepository kieRepository = getKieServices().getRepository();
		LOGGER.info(
				"Drools Engine Service initialization for '{}' module finished. ReleaseId of the new Kie Module: '{}'",
				module.getName(), kieModule.getReleaseId().toExternalForm());
		kieRepository.addKieModule(kieModule);
		final KieContainer kieContainer = getKieServices().newKieContainer(releaseId);
		result.setDeployedVersion(kieContainer.getReleaseId().getVersion());
		return kieContainer;
	}

	private void initializeNewModule(final RulesModule module,
			final KieContainerListener listener, final RuleEngineActionResult result)
	{
		try
		{
			final KieModule newKieModule = createKieModule(module, result);
			listener.onSuccess(initializeNewKieContainer(module, newKieModule, result));
		}
		catch (final Throwable e)
		{
			LOGGER.error("DroolsInitializationException occured {}", e);
			completeWithFailure(getReleaseId(module), result, listener);
		}
	}

	protected void completeWithFailure(final ReleaseId releaseId, final RuleEngineActionResult result,
			final KieContainerListener listener)
	{
		final KieRepository kieRepository = getKieServices().getRepository();
		final KieModule corruptedKieModule = kieRepository.getKieModule(releaseId);
		if (nonNull(corruptedKieModule))
		{
			kieRepository.removeKieModule(releaseId);
		}
		result.setActionFailed(true);
		listener.onFailure(result);
	}

	protected void registerWorker(final String moduleName, final Thread worker)
	{
		final Set<Thread> workersForModule = asyncWorkers.get(moduleName);
		Set<Thread> updatedWorkersForModule;
		if (isNull(workersForModule))
		{
			updatedWorkersForModule = ImmutableSet.of(worker);
		}
		else
		{
			final Set<Thread> aliveWorkers = workersForModule.stream().filter(Thread::isAlive).collect(toSet());
			aliveWorkers.add(worker);
			updatedWorkersForModule = ImmutableSet.copyOf(aliveWorkers);
		}
		asyncWorkers.put(moduleName, updatedWorkersForModule);
	}

	protected Runnable switchKieModuleRunnableTask(final String moduleName,
			final KieContainerListener listener, final List<Object> resultsAccumulator, final Supplier<Object> resetFlagSupplier,
			final List<Supplier<Object>> postTaskList,
			final RuleEngineActionResult result)
	{
		checkArgument(nonNull(resultsAccumulator), "Results accumulator must be initialized upfront");

		return () ->
		{
			try
			{
				final RulesModule module = RulesModuleRepo.findByName(moduleName);
				resultsAccumulator.addAll(switchKieModule(module, listener, (LinkedList<Supplier<Object>>) postTaskList, result));
				return;
			}
			catch (final Exception e)
			{
				onSwapFailed(e, result, resetFlagSupplier);
			}
			result.setModuleName(moduleName);
			result.setActionFailed(true);
			listener.onFailure(result);
		};
	}

	protected Object onSwapFailed(final Throwable t, final RuleEngineActionResult result, final Supplier<Object> resetFlagSupplier)
	{
		LOGGER.error("Exception caught: {}", t);
		result.setActionFailed(true);
		if (nonNull(resetFlagSupplier))
		{
			return resetFlagSupplier.get();
		}
		return null;
	}

	protected boolean isInitialEngineStartup(final ReleaseId releaseId, final String newDeployedMvnVersion)
	{
		return releaseId.getVersion().equals(newDeployedMvnVersion);
	}

	protected KieServices getKieServices()
	{
		return kieServices;
	}

	protected void setKieServices(final KieServices kieServices)
	{
		this.kieServices = kieServices;
	}

}
