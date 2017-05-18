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
package de.hybris.ruleengine.stage;

import de.hybris.ruleengine.stage.model.ResultItem;

import java.util.List;

import lombok.Data;


@Data
public class RuleEngineActionResult
{
	private String moduleName;
	private boolean actionFailed;
	private String deployedVersion;
	private List<ResultItem> results;
	private String oldVersion;
}
