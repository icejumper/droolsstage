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
package de.hybris.ruleengine.stage.contollers;


import de.hybris.ruleengine.stage.DroolsEqualityBehavior;
import de.hybris.ruleengine.stage.DroolsEventProcessingMode;
import de.hybris.ruleengine.stage.DroolsSessionType;
import de.hybris.ruleengine.stage.RuleEngineActionResult;
import de.hybris.ruleengine.stage.RuleEngineStageService;
import de.hybris.ruleengine.stage.RulesModuleRepo;
import de.hybris.ruleengine.stage.model.DroolsKieSession;
import de.hybris.ruleengine.stage.model.Rule;
import de.hybris.ruleengine.stage.model.RulesBase;
import de.hybris.ruleengine.stage.model.RulesModule;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;



@RestController
public class RuleEngineStageController
{
	private static final Logger LOG = LoggerFactory.getLogger(RuleEngineStageController.class);

	@Autowired
	private RuleEngineStageService ruleEngineStageService;

	@CrossOrigin()
	@RequestMapping(value = "/startRuleEngine/{moduleName}/{numOfRules}", method = RequestMethod.GET)
	public ResponseEntity<String> riskyConnection(@PathVariable String moduleName, @PathVariable int numOfRules)
			throws InterruptedException
	{
		String s = "-> In GET /startRuleEngine/" + moduleName + "/" + numOfRules;
		LOG.info(s);

		final RulesModule rulesModule = createNewRulesModule(moduleName, numOfRules);
		RulesModuleRepo.addRulesModule(rulesModule);

		final Instant start = Instant.now();
		final RuleEngineActionResult result = new RuleEngineActionResult();
		ruleEngineStageService.initialize(rulesModule, null, result);
		final Instant end = Instant.now();
		s += ". Time taken for module initialization for " + numOfRules + " rules: " + Duration.between(start, end).toMillis()
				+ "ms";

		return new ResponseEntity<>(s, HttpStatus.OK);
	}

	private RulesModule createNewRulesModule(final String moduleName, final int numOfRules)
	{
		final RulesModule rulesModule = new RulesModule();
		rulesModule.setName(moduleName);
		rulesModule.setMvnGroupId("test-module-group");
		rulesModule.setMvnArtifactId("test-module-artifact");
		rulesModule.setMvnVersion("1.0.0");
		final RulesBase rulesBase = createNewRulesBase("test-base", rulesModule, numOfRules);
		rulesModule.setKieBases(Lists.newArrayList(rulesBase));
		return rulesModule;
	}

	private RulesBase createNewRulesBase(final String name, final RulesModule rulesModule, final int numOfRules)
	{
		final RulesBase rulesBase = new RulesBase();
		rulesBase.setName(name);
		rulesBase.setEqualityBehavior(DroolsEqualityBehavior.EQUALITY);
		rulesBase.setEventProcessingMode(DroolsEventProcessingMode.STREAM);
		rulesBase.setKieModule(rulesModule);
		LOG.info("Generating {} of sample rules...", numOfRules);
		final Set<Rule> rules = IntStream.range(0, numOfRules).boxed()
				.map(i -> createNewRule(rulesModule.getName(), "fibonacciRule" + i)).collect(
						Collectors.toSet());
		rulesBase.setRules(rules);
		rulesBase.setKieSessions(Lists.newArrayList(createNewSession("test-session")));
		return rulesBase;
	}

	private DroolsKieSession createNewSession(final String name)
	{
		final DroolsKieSession session = new DroolsKieSession();
		session.setName(name);
		session.setSessionType(DroolsSessionType.STATELESS);
		return session;
	}

	private Rule createNewRule(final String moduleName, final String code)
	{
		final InputStream resourceAsStream = this.getClass().getResourceAsStream("/drools_rule_template.drl");
		final String uuid = UUID.randomUUID().toString();
		String ruleContent = null;
		try
		{
			ruleContent = new String(IOUtils.toByteArray(resourceAsStream));
			ruleContent = ruleContent.replace("${module_name}", moduleName).replace("${rule_uuid}", uuid)
					.replace("${rule_code}", code);

		}
		catch (final Exception e)
		{
			Throwables.propagate(e);
		}
		final Rule rule = new Rule();
		rule.setRuleContent(ruleContent);
		rule.setUuid(uuid);
		rule.setActive(true);
		rule.setCode(code);
		return rule;
	}

}
