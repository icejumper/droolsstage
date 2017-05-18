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

import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;


/**
 * helper methods interface for StatelessKieSession
 */
public interface StatelessKieSessionHelper
{

	/**
	 * given the {@link RuleEvaluationContext} and currently active instance of {@link KieContainer}, initializes the {@link
	 * StatelessKieSession} for evaluation of rules
	 *
	 * @param context
	 * 		instance of {@link RuleEvaluationContext}
	 * @param kieContainer
	 * 		currently active instance of {@link KieContainer}
	 * @return initialized instance of {@link StatelessKieSession}
	 */
	StatelessKieSession initializeSession(RuleEvaluationContext context, KieContainer kieContainer);

	/**
	 * Given the {@link RuleEvaluationContext} retrieves the {@link ReleaseId} of the deployed {@link KieContainer}
	 *
	 * @param context
	 * 		instance of {@link RuleEvaluationContext}
	 * @return instance of {@link ReleaseId} of the currently deployed version of {@link KieContainer}
	 */
	ReleaseId getDeployedKieModuleReleaseId(RuleEvaluationContext context);

}
