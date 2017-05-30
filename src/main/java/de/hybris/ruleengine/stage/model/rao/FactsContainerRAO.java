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
package de.hybris.ruleengine.stage.model.rao;

import de.hybris.ruleengine.stage.model.rrd.RuleConfigurationRRD;
import de.hybris.ruleengine.stage.model.rrd.RuleGroupExecutionRRD;

import java.util.Set;

import lombok.Data;


@Data
public class FactsContainerRAO
{
	private CartRAO cartRAO;
	private WebsiteGroupRAO websiteGroupRAO;
	private RuleEngineResultRAO ruleEngineResultRAO;
	private RuleConfigurationRRD ruleConfigurationRRD;
	private RuleGroupExecutionRRD ruleGroupExecutionRRD;
	private Set<OrderEntryRAO> orderEntryRAOList;
	private Set<ProductRAO> productRAOList;

}
