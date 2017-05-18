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
package de.hybris.ruleengine.stage.init;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import de.hybris.ruleengine.stage.RuleEngineActionResult;
import de.hybris.ruleengine.stage.concurrency.TaskResult;

import java.util.List;

/**
 * Rules-deployment specific implementation for the {@link TaskResult}
 */
public class RuleDeploymentTaskResult implements TaskResult
{

	private final List<RuleEngineActionResult> rulePublishingResults;

	public RuleDeploymentTaskResult(final List<RuleEngineActionResult> rulePublishingResults)
	{
		this.rulePublishingResults = rulePublishingResults;
	}

	public List<RuleEngineActionResult> getRulePublishingResults()
	{
		return rulePublishingResults;
	}

	@Override
	public State getState()
	{
		State state = State.SUCCESS;
		final List<RuleEngineActionResult> results = getRulePublishingResults();
		if (isNotEmpty(results) && results.stream().anyMatch(RuleEngineActionResult::isActionFailed))
		{
			state = State.FAILURE;
		}
		return state;
	}
}
