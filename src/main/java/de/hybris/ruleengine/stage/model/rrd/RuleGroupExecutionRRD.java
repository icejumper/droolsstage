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


@Data
public class RuleGroupExecutionRRD
{
	private String code;
	private boolean mayExecute = false;

	public boolean allowedToExecute(final RuleConfigurationRRD ruleConfigurationRRD)
	{
		mayExecute = mayExecute?false:true;
		return mayExecute;
	}
}
