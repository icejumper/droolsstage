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
package de.hybris.ruleengine.stage.concurrency.impl;

import de.hybris.ruleengine.stage.concurrency.TaskContext;

import java.util.concurrent.ThreadFactory;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.ThreadFactoryBuilder;


/**
 * Default implementation of the {@link TaskContext} interface
 */
@Component
public class DefaultTaskContext implements TaskContext
{

	private static final String WORKER_PRE_DESTROY_TIMEOUT = "ruleengine.task.predestroytimeout";     // NOSONAR

	private ThreadFactory threadFactory;

	@Override
	public ThreadFactory getThreadFactory()
	{
		return threadFactory;
	}

	@Override
	public int getNumberOfThreads()
	{
		return Runtime.getRuntime().availableProcessors() - 1;
	}

	@Override
	public Long getThreadTimeout()
	{
		return 3600000L;
	}

	@PostConstruct
	private void setUp()
	{
		threadFactory = new ThreadFactoryBuilder().build();
	}

}
