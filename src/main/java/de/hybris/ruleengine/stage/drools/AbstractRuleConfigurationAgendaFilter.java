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
package de.hybris.ruleengine.stage.drools;

import de.hybris.ruleengine.stage.model.rrd.RuleConfigurationRRD;
import de.hybris.ruleengine.stage.utils.RuleEngineConstants;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.drools.core.common.InternalFactHandle;
import org.kie.api.definition.KieDefinition.KnowledgeType;
import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.rule.AgendaFilter;
import org.kie.api.runtime.rule.Match;


/**
 * AbstractRuleConfigurationAgendaFilter provides an abstract base class for agenda filters that need to make use of
 * {@link RuleConfigurationRRD} objects.
 */
public abstract class AbstractRuleConfigurationAgendaFilter implements AgendaFilter
{

	/**
	 * returns the RuleConfigurationRRD for the given match by looking up the rules ruleCode meta data.
	 */
	protected Optional<RuleConfigurationRRD> getRuleConfig(final Match match)
	{
		final Rule rule = match.getRule();

		if (rule.getKnowledgeType() != KnowledgeType.RULE)
		{
			return Optional.empty();
		}

		final List<RuleConfigurationRRD> ruleConfigs = match.getFactHandles().stream()
				.filter(fact -> fact instanceof InternalFactHandle).map(fact -> ((InternalFactHandle) fact).getObject())
				.filter(fact -> fact instanceof RuleConfigurationRRD).map(fact -> (RuleConfigurationRRD) fact)
				.collect(Collectors.toList());

		if (ruleConfigs.isEmpty())
		{
			return Optional.empty();
		}

		final String ruleCode = (String) rule.getMetaData().get(RuleEngineConstants.RULEMETADATA_RULECODE);
		if (StringUtils.isEmpty(ruleCode))
		{
			throw new IllegalStateException("Misconfigured rule: @ruleCode is not set or empty for drools rule:" + rule.getName()
					+ " in package:" + rule.getPackageName());
		}

		return ruleConfigs.stream().filter(config -> ruleCode.equals(config.getRuleCode())).findFirst();
	}

	/**
	 * looks up the corresponding RuleConfigurationRRD and if found, invokes {@link #accept(Match, RuleConfigurationRRD)}
	 * with it.
	 */
	@Override
	public boolean accept(final Match match)
	{
		final Optional<RuleConfigurationRRD> option = getRuleConfig(match);
		if (option.isPresent())
		{
			return accept(match, option.get());
		}
		return true;
	}

	/**
	 * is invoked by this class' {@link #accept(Match)} method.
	 */
	protected abstract boolean accept(Match match, RuleConfigurationRRD ruleConfigurationRRD);

}
