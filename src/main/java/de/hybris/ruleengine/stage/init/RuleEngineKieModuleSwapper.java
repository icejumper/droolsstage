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

import de.hybris.ruleengine.stage.DroolsEqualityBehavior;
import de.hybris.ruleengine.stage.DroolsEventProcessingMode;
import de.hybris.ruleengine.stage.DroolsSessionType;
import de.hybris.ruleengine.stage.RuleEngineActionResult;
import de.hybris.ruleengine.stage.model.DroolsKieSession;
import de.hybris.ruleengine.stage.model.RulesBase;
import de.hybris.ruleengine.stage.model.RulesModule;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.KieContainer;


/**
 * Drools - specific interface incapsulating the logic of swapping to a new KieContainer during the RuleEngine
 * initialization phase
 */
public interface RuleEngineKieModuleSwapper
{

	List<Object> switchKieModule(RulesModule module, KieContainerListener listener,
			LinkedList<Supplier<Object>> postTaskList, RuleEngineActionResult result);


	void switchKieModuleAsync(String moduleName,
			KieContainerListener listener, List<Object> resultsAccumulator, Supplier<Object> resetFlagSupplier,
			List<Supplier<Object>> postTaskList, RuleEngineActionResult result);


	/**
	 * Creates the new instance of {@link KieContainer} for a given {@link KieModule}
	 *
	 * @param module
	 * 		instance of {@link RulesModule}
	 * @param kieModule
	 * 		instance of {@link KieModule}
	 * @param result
	 * 		instance of {@link RuleEngineActionResult} to be used in cluster nodes notification
	 * @return new {@link KieContainer} instance
	 */
	KieContainer initializeNewKieContainer(RulesModule module, KieModule kieModule, RuleEngineActionResult result);

	/**
	 * Creates the new instance of {@link KieModule}, based on information contained in {@link RulesModule}
	 *
	 * @param module
	 * 		instance of {@link RulesModule}
	 * @param result
	 * 		instance of {@link RuleEngineActionResult} to be used in cluster nodes notification
	 * @return pair of the newly created instance of {@link KieModule} and the corresponding caching structure for the
	 * created module
	 */
	KieModule createKieModule(RulesModule module, RuleEngineActionResult result);

	/**
	 * Adds new {@link KieBaseModel} to a {@link KieModuleModel} with all rules
	 *
	 * @param module
	 * 		instance of {@link KieModuleModel} to add the {@link KieBaseModel} to
	 * @param kfs
	 * 		instance of {@link KieFileSystem}
	 * @param base
	 * 		instance of {@link RulesBase} that keeps the information for a {@link KieBaseModel} to be
	 * 		created
	 */
	void addKieBase(KieModuleModel module, KieFileSystem kfs, RulesBase base);

	/**
	 * Adds new {@link KieBaseModel} to a {@link KieModuleModel} with all rules
	 *
	 * @param module
	 * 		instance of {@link KieModuleModel} to add the {@link KieBaseModel} to
	 * @param base
	 * 		instance of {@link RulesBase} that keeps the information for a {@link KieBaseModel} to be
	 * 		created
	 */
	void addKieBase(KieModuleModel module, RulesBase base);


	/**
	 * Updates the instance of {@link RulesModule} with information about affectively deployed {@link ReleaseId}
	 * version
	 *
	 * @param module
	 * 		instance of {@link RulesModule}
	 * @return version of deployed {@link ReleaseId}
	 */
	String activateKieModule(RulesModule module);

	/**
	 * Tries to remove the {@link KieModule} with given {@link ReleaseId} from {@link org.kie.api.builder.KieRepository}
	 *
	 * @param releaseId
	 * 		the instance of {@link ReleaseId} corresponding to a {@link KieModule} to be removed
	 * @param result
	 * 		instance of {@link RuleEngineActionResult} to be used in cluster nodes notification
	 * 		removal
	 * @return true if the module was found and removed, false otherwise
	 */
	boolean removeKieModuleIfPresent(final ReleaseId releaseId, RuleEngineActionResult result);

	/**
	 * Tries to remove the old {@link KieModule} from {@link org.kie.api.builder.KieRepository}
	 *
	 * @param result
	 * 		instance of {@link RuleEngineActionResult} to be used in cluster nodes notification
	 * 		removal
	 * @return true if the module was found and removed, false otherwise
	 */
	boolean removeOldKieModuleIfPresent(final RuleEngineActionResult result);

	/**
	 * Adds instance of new {@link KieSessionModel} to {@link KieBaseModel}
	 *
	 * @param base
	 * 		instance of {@link KieBaseModel}
	 * @param session
	 * 		instance of {@link DroolsKieSession} containing the information for new {@link KieSessionModel}
	 */
	void addKieSession(KieBaseModel base, DroolsKieSession session);

	/**
	 * Adds rules from a given {@link RulesBase} to {@link KieFileSystem}
	 *
	 * @param kfs
	 * 		instance of {@link KieFileSystem}
	 * @param base
	 * 		instance of {@link RulesBase} containing the reference to the rules to publish
	 */
	void addRules(KieFileSystem kfs, RulesBase base);

	/**
	 * Creates the XML representation of {@link KieModuleModel} and writes it to {@link KieFileSystem}
	 *
	 * @param module
	 * 		instance of {@link KieModuleModel}
	 * @param kfs
	 * 		instance of {@link KieFileSystem}
	 */
	void writeKModuleXML(KieModuleModel module, KieFileSystem kfs);

	/**
	 * Write the building POM XML to {@link KieFileSystem}
	 *
	 * @param module
	 * 		instance of {@link RulesModule} to be used for {@link ReleaseId} creation
	 * @param kfs
	 * 		instance of {@link KieFileSystem}
	 */
	void writePomXML(RulesModule module, KieFileSystem kfs);

	/**
	 * Creates the new instance of {@link ReleaseId} based on {@link RulesModule}
	 *
	 * @param module
	 * 		instance of {@link RulesModule}
	 * @return newly created {@link ReleaseId}
	 */
	ReleaseId getReleaseId(RulesModule module);

	/**
	 * Returns (optional) {@link ReleaseId} for a deployed version of the {@link KieModuleModel}
	 *
	 * @param module
	 * 		instance of {@link RulesModule}
	 * @param deployedMvnVersion
	 * 		currently deployed releaseId version of the Kie Module, if known
	 * @return instance of {@link Optional}.of({@link ReleaseId}) if the {@link ReleaseId} could be created,
	 * {@link Optional}.empty() otherwise
	 */
	Optional<ReleaseId> getDeployedReleaseId(RulesModule module, String deployedMvnVersion);


	/**
	 * Initializes the {@link org.kie.api.KieServices} instance
	 */
	void setUpKieServices();

	/**
	 * Block until the whole swapping task/tasks are finished
	 */
	void waitForSwappingToFinish();

	/**
	 * converts between hybris and drools session type
	 */
	static KieSessionModel.KieSessionType getSessionType(final DroolsSessionType sessionType)
	{
		switch (sessionType)
		{
			case STATEFUL:
				return KieSessionModel.KieSessionType.STATEFUL;
			case STATELESS:
				return KieSessionModel.KieSessionType.STATELESS;
			default:
				return null;
		}
	}

	/**
	 * converts between hybris and drools equality behavior
	 */
	static EqualityBehaviorOption getEqualityBehaviorOption(final DroolsEqualityBehavior behavior)
	{
		switch (behavior)
		{
			case EQUALITY:
				return EqualityBehaviorOption.EQUALITY;
			case IDENTITY:
				return EqualityBehaviorOption.IDENTITY;
			default:
				return null;
		}
	}

	/**
	 * converts between hybris and drools event processing mode
	 */
	static EventProcessingOption getEventProcessingOption(final DroolsEventProcessingMode eventProcessingMode)
	{
		switch (eventProcessingMode)
		{
			case STREAM:
				return EventProcessingOption.STREAM;
			case CLOUD:
				return EventProcessingOption.CLOUD;
			default:
				return null;
		}
	}

}
