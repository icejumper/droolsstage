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
package de.hybris.ruleengine.stage.actions;

import org.drools.core.spi.KnowledgeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OrderPercentageDiscountRAOAction extends AbstractRAOAction
{

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderPercentageDiscountRAOAction.class);

	@Override
	public void perform(final KnowledgeHelper context)
	{
		LOGGER.info("Applying discount in {}", this.getClass().getName());
		trackRuleGroupExecutions(context);
	}

}
