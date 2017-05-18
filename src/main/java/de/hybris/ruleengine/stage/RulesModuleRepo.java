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
package de.hybris.ruleengine.stage;

import de.hybris.ruleengine.stage.model.Rule;
import de.hybris.ruleengine.stage.model.RulesModule;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;


public class RulesModuleRepo
{
	private static Map<String, RulesModule> rulesModuleMap = Maps.newConcurrentMap();
	private static Map<String, Rule> rulesMap = Maps.newConcurrentMap();

	public static RulesModule findByName(final String moduleName)
	{
		return rulesModuleMap.get(moduleName);
	}

	public static Map<String, RulesModule> getRulesModuleMap()
	{
		return rulesModuleMap;
	}

	public static void addRulesModule(final RulesModule rulesModule)
	{
		rulesModuleMap.put(rulesModule.getName(), rulesModule);
		rulesModule.getKieBases().stream().flatMap(b -> b.getRules().stream()).forEach(r -> rulesMap.put(r.getUuid(), r));
	}

	public static List<Rule> getRulesByUuids(List<String> uuids)
	{
		return uuids.stream().map(rulesMap::get).collect(Collectors.toList());
	}

}
