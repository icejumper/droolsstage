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

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.util.Set;

import com.google.common.base.Throwables;


/**
 * Interface for the task execution future to keep the state of the executing task/threads
 */
public interface TaskExecutionFuture<T extends TaskResult>
{

	T getTaskResult();

	long getWorkerPreDestroyTimeout();

	void waitForTasksToFinish();

	default void waitForTasksToFinish(final Set<Thread> workers)
	{
		if (isNotEmpty(workers))
		{
			workers.forEach(this::waitWhileWorkerIsRunning);
		}
	}

	default void waitWhileWorkerIsRunning(final Thread worker)
	{
		if (nonNull(worker) && worker.isAlive())
		{
			try
			{
				worker.join(getWorkerPreDestroyTimeout());
			}
			catch (final InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw Throwables.propagate(e);
			}
		}
	}

}
