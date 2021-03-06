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

import java.io.Serializable;
import java.util.Set;

import lombok.Data;


@Data
public class UserRAO implements Serializable
{
	private String id;
	private Set<CartRAO> orders;
	private Set<UserGroupRAO> groups;
}
