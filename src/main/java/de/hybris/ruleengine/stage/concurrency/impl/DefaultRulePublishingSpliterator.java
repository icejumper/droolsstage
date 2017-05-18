/*
 * [y] hybris Platform
 * 
 * Copyright (c) 2017 SAP SE or an SAP affiliate company. All rights reserved.
 * 
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.ruleengine.stage.concurrency.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newCopyOnWriteArrayList;
import static com.google.common.collect.Lists.partition;
import static com.google.common.collect.Sets.newHashSet;
import static de.hybris.ruleengine.stage.utils.RuleEngineUtils.getRulePath;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

import de.hybris.ruleengine.stage.RuleEngineActionResult;
import de.hybris.ruleengine.stage.RulesModuleRepo;
import de.hybris.ruleengine.stage.concurrency.RulePublishingFuture;
import de.hybris.ruleengine.stage.concurrency.RulePublishingSpliterator;
import de.hybris.ruleengine.stage.concurrency.TaskContext;
import de.hybris.ruleengine.stage.init.RuleEngineBootstrap;
import de.hybris.ruleengine.stage.model.Rule;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PostConstruct;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Default implementation of {@link RulePublishingSpliterator}
 */
@Component
public class DefaultRulePublishingSpliterator implements RulePublishingSpliterator
{
	private static final String BASE_WORKER_NAME = "RulePublisher";

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRulePublishingSpliterator.class);

	private KieServices kieServices;
	@Autowired
	private TaskContext taskContext;
	@Autowired
	private RuleEngineBootstrap<KieServices, KieContainer> ruleEngineBootstrap;

	@Override
	public RulePublishingFuture publishRulesAsync(final KieModuleModel kieModuleModel, final ReleaseId containerReleaseId,
			final List<String> ruleUuids)
	{
		final List<KieBuilder> kieBuilders = new CopyOnWriteArrayList<>();

		LOGGER.info("Number of threads: {}", taskContext.getNumberOfThreads());

		final List<List<String>> partitionOfRulesUuids = splitListByThreads(ruleUuids, taskContext.getNumberOfThreads());

		final Set<Thread> builderWorkers = newHashSet();
		final List<RuleEngineActionResult> ruleEngineActionResults = newCopyOnWriteArrayList();
		for (final List<String> ruleUuidsChunk : partitionOfRulesUuids)
		{
			builderWorkers.add(createNewWorker(kieBuilders, kieModuleModel, containerReleaseId, ruleUuidsChunk,
					ruleEngineActionResults));
		}
		startWorkers(builderWorkers);
		return new RulePublishingFuture(builderWorkers, ruleEngineActionResults, kieBuilders, taskContext.getThreadTimeout());
	}

	@PostConstruct
	protected void setUp()
	{
		kieServices = ruleEngineBootstrap.getEngineServices();
	}

	private <T> List<List<T>> splitListByThreads(final List<T> list, final int numberOfThreads)
	{
		checkArgument(numberOfThreads > 0,
				"Valid maximum number of threads (>0) must be provided");

		final int partitionSize = getPartitionSize(list.size(), numberOfThreads);
		if (partitionSize == 0)
		{
			return Collections.emptyList();
		}
		return partition(list, partitionSize);
	}

	private Thread createNewWorker(final List<KieBuilder> kieBuilders, final KieModuleModel kieModuleModel,
			final ReleaseId releaseId, final List<String> ruleUuids,
			final List<RuleEngineActionResult> ruleEngineActionResults)
	{
		final ThreadFactory tenantAwareThreadFactory = taskContext.getThreadFactory();

		checkArgument(nonNull(tenantAwareThreadFactory), "ThreadFactory must be provided as part of TaskContext");

		return tenantAwareThreadFactory.newThread(
				() -> ruleEngineActionResults
						.add(addRulesBuilder(kieBuilders, kieModuleModel, releaseId, ruleUuids)));
	}

	private void startWorkers(final Set<Thread> workers)
	{
		if (isNotEmpty(workers))
		{
			for (final Thread worker : workers)
			{
				worker.setName(BASE_WORKER_NAME + "-" + worker.getName());
				worker.start();
			}
		}
	}

	private RuleEngineActionResult addRulesBuilder(final List<KieBuilder> kieBuilders, final KieModuleModel kieModuleModel,
			final ReleaseId releaseId, final List<String> ruleUuids)
	{
		final Collection<Rule> droolRules = RulesModuleRepo.getRulesByUuids(ruleUuids);

		final KieFileSystem partialKieFileSystem = getKieServices().newKieFileSystem();
		writeKModuleXML(kieModuleModel, partialKieFileSystem);
		writePomXML(releaseId, partialKieFileSystem);

		final KieBuilder partialKieBuilder = getKieServices().newKieBuilder(partialKieFileSystem);
		for (final Rule rule : droolRules)
		{
			if (nonNull(rule.getRuleContent()) && isTrue(rule.getActive()))
			{
				partialKieFileSystem.write(getRulePath(rule), rule.getRuleContent());
			}
			if (isNull(rule.getRuleContent()))
			{
				LOGGER.warn("ignoring rule {}. No ruleContent set!", rule.getCode());
			}
			else if (isNotTrue(rule.getActive()))
			{
				LOGGER.debug("ignoring rule {}. Rule is not active.", rule.getCode());
			}
		}
		partialKieBuilder.buildAll();
		kieBuilders.add(partialKieBuilder);
		return createNewResult(partialKieBuilder.getResults());
	}

	private void writeKModuleXML(final KieModuleModel module, final KieFileSystem kfs)
	{
		kfs.writeKModuleXML(module.toXML());
	}

	private void writePomXML(final ReleaseId releaseId, final KieFileSystem kfs)
	{
		kfs.generateAndWritePomXML(releaseId);
	}

	private RuleEngineActionResult createNewResult(final Results results)
	{
		final RuleEngineActionResult ruleEngineActionResult = new RuleEngineActionResult();
		if (results.hasMessages(Message.Level.ERROR))
		{
			ruleEngineActionResult.setActionFailed(true);
		}
		return ruleEngineActionResult;
	}

	private KieServices getKieServices()
	{
		return kieServices;
	}

	private static int getPartitionSize(final int totalSize, final int numberOfPartitions)
	{
		int partitionSize = totalSize;
		if (numberOfPartitions > 1 && partitionSize >= numberOfPartitions * numberOfPartitions)
		{
			if (totalSize % numberOfPartitions == 0)
			{
				partitionSize = totalSize / numberOfPartitions;
			}
			else if (numberOfPartitions > 1)
			{
				partitionSize = totalSize / (numberOfPartitions - 1);
			}
		}
		return partitionSize;
	}

}
