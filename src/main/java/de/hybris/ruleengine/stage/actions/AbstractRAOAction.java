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
package de.hybris.ruleengine.stage.actions;

import static java.util.Objects.isNull;

import de.hybris.ruleengine.stage.model.rrd.RuleConfigurationRRD;
import de.hybris.ruleengine.stage.model.rrd.RuleGroupExecutionRRD;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.spi.KnowledgeHelper;
import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.rule.FactHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractRAOAction implements RAOAction
{

	private static final Logger LOG = LoggerFactory.getLogger(AbstractRAOAction.class);

	protected void trackRuleGroupExecutions(final KnowledgeHelper context)
	{
		final String ruleCode = getMetaDataFromRule(context.getRule(), "ruleCode");
		if (ruleCode == null)
		{
			LOG.error(
					"cannot track rule group execution as current rule:" + context.getRule().getId()
							+ " has no rule code defined!");
			return;
		}
		final RuleConfigurationRRD config = getRuleConfigurationRRD(ruleCode, context);

		if (config != null)
		{
			final String ruleGroupCode = config.getRuleGroupCode();
			if (StringUtils.isNotEmpty(ruleGroupCode))
			{
				final RuleGroupExecutionRRD execution = getRuleGroupExecutionRRD(ruleGroupCode, context);
				if (execution != null)
				{
					trackRuleGroupExecution(execution, config);
					updateFacts(context, execution);
				}
			}
		}
	}

	private String getMetaDataFromRule(final Rule rule, final String key)
	{
		final Object value = rule.getMetaData().get(key);
		return value == null ? null : value.toString();
	}

	protected void trackRuleExecution(final KnowledgeHelper context)
	{
		final String ruleCode = getMetaDataFromRule(context.getRule(), "ruleCode");
		if (isNull(ruleCode))
		{
			return;
		}
		final RuleConfigurationRRD config = getRuleConfigurationRRD(ruleCode, context);
		if (config != null)
		{
			config.setCurrentRuns(Integer.valueOf(config.getCurrentRuns() == null ? 1 : config.getCurrentRuns().intValue() + 1));
			updateFacts(context, config);
		}
	}

	private void trackRuleGroupExecution(final RuleGroupExecutionRRD execution, final RuleConfigurationRRD config)
	{
		final String ruleCode = config.getRuleCode();
		if (execution.getExecutedRules() == null)
		{
			final Map<String, Integer> executedRules = new LinkedHashMap<>();
			executedRules.put(ruleCode, Integer.valueOf(1));
			execution.setExecutedRules(executedRules);
		}
		else
		{
			if (execution.getExecutedRules().containsKey(ruleCode))
			{
				execution.getExecutedRules().put(ruleCode,
						Integer.valueOf(execution.getExecutedRules().get(ruleCode).intValue() + 1));
			}
			else
			{
				execution.getExecutedRules().put(ruleCode, Integer.valueOf(1));
			}
		}
	}

	private RuleConfigurationRRD getRuleConfigurationRRD(final String ruleCode, final KnowledgeHelper context)
	{
		RuleConfigurationRRD config = null;
		// returns this rule's RuleConfigurationRRD object
		// returns this rule's RuleConfigurationRRD object
		final Collection<FactHandle> configFacts = context.getWorkingMemory().getFactHandles(
				object -> object instanceof RuleConfigurationRRD && ruleCode.equals(((RuleConfigurationRRD) object).getRuleCode()));
		if (configFacts.size() == 1)
		{
			config = (RuleConfigurationRRD) ((InternalFactHandle) configFacts.iterator().next()).getObject();
		}
		else
		{
			LOG.error("cannot track rule group execution as current rule has {} corresponding RuleConfigurationRRD objects.",
					configFacts.size());
		}
		return config;
	}

	private RuleGroupExecutionRRD getRuleGroupExecutionRRD(final String ruleGroupCode, final KnowledgeHelper context)
	{
		// returns all RuleGroupExecutionRRD objects matching
		final Collection<FactHandle> factHandles = context.getWorkingMemory().getFactHandles(
				object -> object instanceof RuleGroupExecutionRRD && ruleGroupCode
						.equals(((RuleGroupExecutionRRD) object).getCode()));
		if (factHandles != null && factHandles.size() == 1)
		{
			return (RuleGroupExecutionRRD) ((InternalFactHandle) factHandles.iterator().next()).getObject();
		}
		else
		{
			final String count = factHandles == null ? "no" : Integer.toString(factHandles.size());
			LOG.error(
					"cannot track rule group execution as rule group for code {} has {}  corresponding RuleGroupExecutionRRD objects.",
					ruleGroupCode, count);
			return null;
		}
	}

	protected <T> Collection<T> getFactsOfType(final Class<T> clazz, final KnowledgeHelper context)
	{
		final Collection factHandles = context.getWorkingMemory().getFactHandles(clazz::isInstance);
		if (CollectionUtils.isNotEmpty(factHandles))
		{
			return (Collection<T>)factHandles.stream().map(h -> ((InternalFactHandle)h).getObject()).collect(Collectors.toList());
		}
		return null;
	}

	private void updateFacts(final Object engineContext, final Object... facts)
	{
		final KnowledgeHelper helper = (KnowledgeHelper) engineContext;
		for (final Object fact : facts)
		{
			helper.update(fact);
		}
	}
}
