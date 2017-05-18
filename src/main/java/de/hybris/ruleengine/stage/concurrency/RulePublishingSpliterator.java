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
package de.hybris.ruleengine.stage.concurrency;

import java.util.List;

import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieModuleModel;


/**
 * interface for publishing task spliterator, accumulating and splitting the rules to be published
 */
public interface RulePublishingSpliterator
{


	/**
	 * split and compile multiple rules in parallel
	 *
	 * @param kieModuleModel
	 * 		instance of {@link KieModuleModel}
	 * @param containerReleaseId
	 * 		Kie container release id {@link ReleaseId}
	 * @param ruleUuids
	 * 		a list of rule uuids
	 * @return instance of {@link RulePublishingFuture}
	 */
	RulePublishingFuture publishRulesAsync(KieModuleModel kieModuleModel, ReleaseId containerReleaseId,
			List<String> ruleUuids);

}
