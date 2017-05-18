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
package de.hybris.ruleengine.stage.model;

import de.hybris.ruleengine.stage.DroolsEqualityBehavior;
import de.hybris.ruleengine.stage.DroolsEventProcessingMode;

import java.util.List;
import java.util.Set;

import lombok.Data;

@Data
public class RulesBase
{
	private String name;
	private RulesModule kieModule;
	private Set<Rule> rules;
	private DroolsEqualityBehavior equalityBehavior;
	private DroolsEventProcessingMode eventProcessingMode;
	private List<DroolsKieSession> kieSessions;
}
