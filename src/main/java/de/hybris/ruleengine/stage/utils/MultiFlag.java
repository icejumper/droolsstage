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
package de.hybris.ruleengine.stage.utils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;


/**
 * Keeps the state of AtomicBoolean flag for multiple keys, providing the atomic access to all read/update operations
 */
public class MultiFlag
{
	private static final Logger LOGGER = LoggerFactory.getLogger(MultiFlag.class);

	private final Map<String, AtomicBoolean> keyToFlagMap;

	public MultiFlag()
	{
		this.keyToFlagMap = Maps.newConcurrentMap();
	}

	public boolean compareAndSet(final String key, final boolean expected, final boolean update)
	{
		final AtomicBoolean flagForKey = keyToFlagMap.computeIfAbsent(key, k -> new AtomicBoolean(false));
		final boolean result = flagForKey.compareAndSet(expected, update);
		LOGGER.debug("MultiFlag.compareAndSet called with:  module: {}, expected:{}, update:{}, result:{}", key, expected, update,
				result);
		return result;
	}

}
