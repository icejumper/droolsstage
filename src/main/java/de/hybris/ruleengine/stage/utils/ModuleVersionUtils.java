package de.hybris.ruleengine.stage.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.nonNull;

import de.hybris.ruleengine.stage.model.RulesModule;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class ModuleVersionUtils
{

	private final ReadWriteLock readWriteLock;
	private final Lock readLock;
	private final Lock writeLock;

	private byte deployedModuleVersion = 0;

	public ModuleVersionUtils()
	{
		readWriteLock = new ReentrantReadWriteLock();
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();
	}

	protected byte swapModuleVersion()
	{
		writeLock.lock();
		try
		{
			deployedModuleVersion ^= 1;
			return deployedModuleVersion;
		}
		finally
		{
			writeLock.unlock();
		}
	}

	public String getDeployedRulesModuleVersion(final RulesModule rulesModule, final boolean swap)
	{
		if (swap)
		{
			swapModuleVersion();
		}
		readLock.lock();
		try
		{
			checkArgument(nonNull(rulesModule), "Rules module shouldn't be null here");

			final String mvnVersion = rulesModule.getMvnVersion();
			final int idxOfLastDot = mvnVersion.lastIndexOf('.');
			if (idxOfLastDot != -1)
			{
				return mvnVersion.substring(0, idxOfLastDot) + "." + deployedModuleVersion;
			}
			return mvnVersion + "." + deployedModuleVersion;
		}
		finally
		{
			readLock.unlock();
		}
	}

}
