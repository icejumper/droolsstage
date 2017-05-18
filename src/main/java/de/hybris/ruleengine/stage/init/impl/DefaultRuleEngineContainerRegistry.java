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
package de.hybris.ruleengine.stage.init.impl;

import de.hybris.ruleengine.stage.init.RuleEngineContainerRegistry;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;


/**
 * Default implementation of {@link RuleEngineContainerRegistry} interface, base on Drools
 */
@Component
public class DefaultRuleEngineContainerRegistry implements RuleEngineContainerRegistry<ReleaseId, KieContainer>
{

	private Map<ReleaseId, KieContainer> kieContainerMap = Maps.newConcurrentMap();

	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final Lock readLock = readWriteLock.readLock();
	private final Lock writeLock = readWriteLock.writeLock();

	@Override
	public void setActiveContainer(final ReleaseId releaseId, final KieContainer rulesContainer)
	{
		kieContainerMap.put(releaseId, rulesContainer);
	}

	@Override
	public KieContainer getActiveContainer(final ReleaseId releaseId)
	{
		return kieContainerMap.get(releaseId);
	}

	@Override
	public Optional<ReleaseId> lookupForDeployedRelease(final String... releaseTokens)
	{
		Preconditions.checkArgument(Objects.nonNull(releaseTokens), "Lookup release tokens should be provided");
		if(releaseTokens.length == 2)
		{
			return kieContainerMap.keySet().stream()
					.filter(rid -> rid.getGroupId().equals(releaseTokens[0]) && rid.getArtifactId()
							.equals(releaseTokens[1])).findFirst();
		}
		return Optional.empty();
	}

	@Override
	public KieContainer removeActiveContainer(final ReleaseId releaseHolder)
	{
		return kieContainerMap.remove(releaseHolder);
	}

	@Override
	public void lockReadingRegistry()
	{
		readLock.lock();
	}

	@Override
	public void unlockReadingRegistry()
	{
		readLock.unlock();
	}

	@Override
	public void lockWritingRegistry()
	{
		writeLock.lock();
	}

	@Override
	public void unlockWritingRegistry()
	{
		writeLock.unlock();
	}

	@Override
	public boolean isLockedForReading()
	{
		return readWriteLock.getReadLockCount() > 0;
	}

	@Override
	public boolean isLockedForWriting()
	{
		return readWriteLock.isWriteLocked();
	}
	
	protected ReadWriteLock getReadWriteLock()
	{
		return readWriteLock;
	}

	protected Lock getReadLock()
	{
		return readLock;
	}

	protected Lock getWriteLock()
	{
		return writeLock;
	}

}
