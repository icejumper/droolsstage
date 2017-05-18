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

import java.util.concurrent.ThreadFactory;


/**
 * The interface for the rule compilation context
 */
public interface TaskContext
{

	/**
	 * get the thread factory to be used for creating the tenant-aware threads
	 *
	 * @return the thread factory implementation
	 */
	ThreadFactory getThreadFactory();

	/**
	 * get the maximum number of threads to allocate for multi-thread execution
	 *
	 * @return number of threads
	 */
	int getNumberOfThreads();

	/**
	 * get the thread pre-destroy timeout
	 *
	 * @return number of milliseconds to wait before forcing the thread to join
	 */
	Long getThreadTimeout();

}
