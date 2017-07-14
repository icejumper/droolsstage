package de.hybris.ruleengine.stage.drools;

import de.hybris.ruleengine.stage.model.rrd.RuleConfigurationRRD;

import org.kie.api.runtime.rule.Match;

public class ActionTriggeringLimitAgendaFilter extends AbstractRuleConfigurationAgendaFilter
{

	@Override
	public boolean accept(final Match match, final RuleConfigurationRRD config)
	{
		final Integer maxAllowedRuns = config.getMaxAllowedRuns();
		final Integer currentRuns = config.getCurrentRuns();
		return currentRuns == null || maxAllowedRuns == null || currentRuns.compareTo(maxAllowedRuns) < 0;
	}
}
