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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@Data
@EqualsAndHashCode(of={"ruleCode"})
@ToString(of={})
public class RuleConfigurationRRD
{
	private String ruleCode;
	private Integer priority;
	private Integer maxAllowedRuns;
	private Integer currentRuns;
	private String ruleGroupCode;

	public void setCurrentRuns(final Integer currentRuns)
	{
		this.currentRuns = currentRuns;
	}
}
