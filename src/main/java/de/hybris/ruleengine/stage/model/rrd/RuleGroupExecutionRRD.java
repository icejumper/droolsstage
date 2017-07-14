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
package de.hybris.ruleengine.stage.model.rrd;

import java.io.Serializable;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(of={"code"})
public class RuleGroupExecutionRRD implements Serializable
{
	private String code;
	private Map<String,Integer> executedRules;

	public boolean allowedToExecute(final RuleConfigurationRRD ruleConfig)
	{
		if (this.executedRules == null)
		{
			// first execution of the group
			return true;
		}
		else
		{
			if (this.executedRules.entrySet().isEmpty())
			{
				// first execution of the group
				return true;
			}
			else
			{
				// no more executions allowed
				// unless this rule has been triggered already and has more than 1 executions allowed
				final Integer current = this.executedRules.get(ruleConfig.getRuleCode());
				if (current == null)
				{
					// this rule hasn't been tracked, so its not allowed to trigger again
					// as another rule has "consumed" this group
					return false;
				}
				Integer max = ruleConfig.getMaxAllowedRuns();
				if (max == null)
				{
					max = Integer.valueOf(1);
				}
				return current.compareTo(max) < 0;
			}
		}
	}
}
