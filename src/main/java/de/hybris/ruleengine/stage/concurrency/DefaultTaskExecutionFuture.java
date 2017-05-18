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

import java.util.Set;


/**
 * Default barebones implementation of {@link TaskExecutionFuture}
 */
public class DefaultTaskExecutionFuture implements TaskExecutionFuture<TaskResult>
{

	private long DEFAULT_PRE_DESTROY_TOUT = 1000L;

	private Set<Thread> workers;
	private long predestroyTimeout;

	public DefaultTaskExecutionFuture(final Set<Thread> workers)
	{
		this(workers, -1);
	}

	public DefaultTaskExecutionFuture(final Set<Thread> workers, final long predestroyTimeout)
	{
		this.workers = workers;
		this.predestroyTimeout = predestroyTimeout;
	}

	@Override
	public TaskResult getTaskResult()
	{
		if(workers.stream().anyMatch(Thread::isAlive))
		{
			return () -> TaskResult.State.IN_PROGRESS;
		}
		return () -> TaskResult.State.SUCCESS;
	}

	@Override
	public long getWorkerPreDestroyTimeout()
	{
		if(predestroyTimeout == -1)
		{
			return DEFAULT_PRE_DESTROY_TOUT;
		}
		return predestroyTimeout;
	}

	@Override
	public void waitForTasksToFinish()
	{
		waitForTasksToFinish(workers);
	}
}
