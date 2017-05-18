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

import de.hybris.ruleengine.stage.RuleEngineActionResult;

import org.kie.api.runtime.KieContainer;


/**
 * The interface for listener to fire every time the KieContainer switch happens. It should encapsulate any logic or
 * sequence of actions (including eventually the necessary blocking locks) to be performed when the new KieContainer is
 * already up and running.
 */
public interface KieContainerListener
{
	/**
	 * The method to be called if the switching operation finished successfully
	 *
	 * @param kieContainer
	 * 		the newly created instance of {@link KieContainer}
	 */
	public void onSuccess(KieContainer kieContainer);

	/**
	 * The method to be called when the switching to a new KieContainer fails
	 *
	 * @param result
	 * 		the switching result accumulating instance of {@link RuleEngineActionResult}
	 */
	public void onFailure(RuleEngineActionResult result);
}
