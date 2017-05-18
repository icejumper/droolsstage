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

import static java.lang.String.format;
import static java.util.Objects.nonNull;

import de.hybris.ruleengine.stage.RuleEngineActionResult;
import de.hybris.ruleengine.stage.RulesModuleRepo;
import de.hybris.ruleengine.stage.init.RuleEngineBootstrap;
import de.hybris.ruleengine.stage.init.RuleEngineContainerRegistry;
import de.hybris.ruleengine.stage.init.RuleEngineKieModuleSwapper;
import de.hybris.ruleengine.stage.model.RulesModule;

import java.util.Objects;
import java.util.Optional;

import org.kie.api.KieServices;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;


/**
 * Default implementation of {@link RuleEngineBootstrap}
 */
@Component
public class DefaultRuleEngineBootstrap implements RuleEngineBootstrap<KieServices, KieContainer>
{

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRuleEngineBootstrap.class);

	@Autowired
	private RuleEngineKieModuleSwapper ruleEngineKieModuleSwapper;
	@Autowired
	private RuleEngineContainerRegistry<ReleaseId, KieContainer> ruleEngineContainerRegistry;

	@Override
	public KieServices getEngineServices()
	{
		return KieServices.Factory.get();
	}

	@Override
	public RuleEngineActionResult startup(final String moduleName)
	{
		Preconditions.checkArgument(nonNull(moduleName), "Module name should be provided");

		final RulesModule rulesModule = RulesModuleRepo.findByName(moduleName);
		if(Objects.nonNull(rulesModule))
		{

			final RuleEngineActionResult result = new RuleEngineActionResult();
			result.setActionFailed(true);
			if (nonNull(rulesModule))
			{
				result.setActionFailed(false);
				final KieModule kieModule = ruleEngineKieModuleSwapper.createKieModule(rulesModule, result);
				final KieContainer kieContainer = ruleEngineKieModuleSwapper.initializeNewKieContainer(rulesModule, kieModule, result);
				activateNewRuleEngineContainer(kieContainer, result, rulesModule, null);
			}
			return result;
		}
		else
		{
			throw new RuntimeException(format("No module with name %s found", moduleName));
		}
	}

	@Override
	public void activateNewRuleEngineContainer(final KieContainer kieContainer,
			final RuleEngineActionResult ruleEngineActionResult, final RulesModule rulesModule,
			final String deployedReleaseIdVersion)
	{
		final ReleaseId releaseId = kieContainer.getReleaseId();
		final Optional<ReleaseId> deployedReleaseId = ruleEngineKieModuleSwapper.getDeployedReleaseId(rulesModule, deployedReleaseIdVersion);
		final String deployedMvnVersion = ruleEngineKieModuleSwapper.activateKieModule(rulesModule);
		LOGGER.info("The new module with deployedMvnVersion [{}] was activated successfully",
				rulesModule.getDeployedMvnVersion());
		LOGGER.info("Swapping to a new created container [{}]", releaseId);
		ruleEngineContainerRegistry.setActiveContainer(releaseId, kieContainer);
		deployedReleaseId.filter(r -> !releaseId.getVersion().equals(r.getVersion()))
				.ifPresent(ruleEngineContainerRegistry::removeActiveContainer);
		ruleEngineActionResult.setDeployedVersion(deployedMvnVersion);
	}

}
