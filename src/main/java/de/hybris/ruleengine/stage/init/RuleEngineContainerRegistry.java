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
package de.hybris.ruleengine.stage.init;


import java.util.Optional;


/**
 * Rule Engine container registry interface. Declares methods for rule engine rules container housekeeping
 *
 * @param <RELEASEHOLDER>
 * 		type of the knowledgebase container module release identifier
 * @param <CONTAINER>
 * 		type of the knowledgebase container
 */
public interface RuleEngineContainerRegistry<RELEASEHOLDER, CONTAINER>
{
	/**
	 * Adds another active container, identified by release, to a repository
	 *
	 * @param releaseHolder
	 * 		knowledgebase container module release identifier
	 * @param rulesContainer
	 * 		knowledgebase container instance
	 */
	void setActiveContainer(RELEASEHOLDER releaseHolder, CONTAINER rulesContainer);

	/**
	 * Retrieve knowledgebase container reference, identified fy release id
	 *
	 * @param releaseHolder
	 * 		knowledgebase container module release identifier
	 * @return knowledgebase container instance
	 */
	CONTAINER getActiveContainer(RELEASEHOLDER releaseHolder);

	/**
	 * Remove knowledgebase container identified by release id
	 *
	 * @param releaseHolder
	 * 		knowledgebase container module release identifier
	 * @return removed knowledge base container instance, null if not found
	 */
	CONTAINER removeActiveContainer(RELEASEHOLDER releaseHolder);

	/**
	 * Lookup for a deployed knowledgebase container release id by partial release id tokens (e.g. group id and artifact id)
	 *
	 * @param releaseTokens
	 * 		knowledgebase container module release identifier
	 * @return optional of container release id
	 */
	Optional<RELEASEHOLDER> lookupForDeployedRelease(String... releaseTokens);

	/**
	 * lock or unlock registry for reading operations
	 */
	void lockReadingRegistry();

	/**
	 * unlock or unlock registry for reading operations
	 */
	void unlockReadingRegistry();

	/**
	 * lock or unlock registry for writing operations
	 */
	void lockWritingRegistry();

	/**
	 * unlock or unlock registry for writing operations
	 */
	void unlockWritingRegistry();

	/**
	 * Check if the registry is locked for reading
	 *
	 * @return true if the registry is in the state locked for reading
	 */
	boolean isLockedForReading();

	/**
	 * Check if the registry is locked for writing
	 *
	 * @return true if the registry is in the state locked for writing
	 */
	boolean isLockedForWriting();

}
