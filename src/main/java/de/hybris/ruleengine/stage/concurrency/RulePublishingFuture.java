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
package de.hybris.ruleengine.stage.concurrency;

import de.hybris.ruleengine.stage.RuleEngineActionResult;
import de.hybris.ruleengine.stage.init.RuleDeploymentTaskResult;

import java.util.List;
import java.util.Set;

import org.kie.api.builder.KieBuilder;


/**
 * The customized implementation for {@link TaskExecutionFuture} for the Rules publishing task
 */
public class RulePublishingFuture extends DefaultTaskExecutionFuture
{

	private final List<RuleEngineActionResult> rulePublishingResults;
	private final List<KieBuilder> partialKieBuilders;
	private final long workerPreDestroyTimeout;

	public RulePublishingFuture(final Set<Thread> workers, final List<RuleEngineActionResult> rulePublishingResults, final List<KieBuilder> partialKieBuilders, final long workerPreDestroyTimeout)
	{
		super(workers);
		this.rulePublishingResults = rulePublishingResults;
		this.workerPreDestroyTimeout = workerPreDestroyTimeout;
		this.partialKieBuilders = partialKieBuilders;
	}

	public List<KieBuilder> getPartialKieBuilders()
	{
		return partialKieBuilders;
	}

	@Override
	public TaskResult getTaskResult()
	{
		waitForTasksToFinish();
		return new RuleDeploymentTaskResult(rulePublishingResults);
	}

	@Override
	public long getWorkerPreDestroyTimeout()
	{
		return workerPreDestroyTimeout;
	}
	
}
