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

import de.hybris.ruleengine.stage.model.rao.CartRAO;

import java.math.BigDecimal;
import java.util.Collection;

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

		final Collection<CartRAO> cartRAOS = getFactsOfType(CartRAO.class, context);
		if(cartRAOS.size() == 1)
		{
			final CartRAO cartRAO = cartRAOS.iterator().next();
			cartRAO.setSubTotal(BigDecimal.valueOf(1000));
			context.update(cartRAO);
		}

		trackRuleExecution(context);
		trackRuleGroupExecutions(context);
	}

}
