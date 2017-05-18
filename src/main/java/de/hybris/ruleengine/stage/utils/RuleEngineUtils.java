/*
 * [y] hybris Platform
 * 
 * Copyright (c) 2017 SAP SE or an SAP affiliate company. All rights reserved.
 * 
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.ruleengine.stage.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static de.hybris.ruleengine.stage.utils.RuleEngineConstants.DROOLS_BASE_PATH;
import static de.hybris.ruleengine.stage.utils.RuleEngineConstants.MEDIA_CODE_POSTFIX;
import static de.hybris.ruleengine.stage.utils.RuleEngineConstants.MEDIA_DRL_FILE_EXTENSION;
import static java.util.Objects.isNull;

import de.hybris.ruleengine.stage.model.Rule;
import de.hybris.ruleengine.stage.model.RulesModule;

import java.io.File;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Static methods for analysis and simple manipulation of the rule content
 */
public class RuleEngineUtils
{

	private static final String RULE_SPLITTING_PATTERN = "(query\\s+.*)(rule\\s+.*)";
	private static Pattern ruleSplittingRegexp;

	static
	{
		ruleSplittingRegexp = Pattern.compile(RULE_SPLITTING_PATTERN, Pattern.DOTALL);
	}

	private RuleEngineUtils()
	{
	}

	public static String getCleanedContent(final String seedRuleContent, final String ruleUuid)
	{
		if (isNull(seedRuleContent))
		{
			return null;
		}
		final Matcher matcher = ruleSplittingRegexp.matcher(seedRuleContent);
		final StringBuilder cleanedContentSB = new StringBuilder();
		if (matcher.find())
		{
			cleanedContentSB.append(matcher.group(1).trim()).append(matcher.group(2).trim());
		}
		else
		{
			cleanedContentSB.append(seedRuleContent.trim());
		}
		final String uuidConcat = ruleUuid.replace("-", "");
		return cleanedContentSB.toString().replace(ruleUuid, "RULE_UUID").replace(uuidConcat, "RULEUUID");
	}

	public static String getNormalizedRulePath(final String rulePath)
	{
		if (isNull(rulePath))
		{
			return null;
		}
		return rulePath.replace(File.separatorChar, '/');
	}

	public static String getRulePath(final Rule rule)
	{
		String rulePackagePath = "";
		if (rule.getRulePackage() != null)
		{
			rulePackagePath = rule.getRulePackage().replace('.', File.separatorChar);
		}
		return getNormalizedRulePath(DROOLS_BASE_PATH + rulePackagePath + rule.getCode() + MEDIA_CODE_POSTFIX
				+ MEDIA_DRL_FILE_EXTENSION);
	}

	public static String stripDroolsMainResources(final String normalizedPath)
	{
		final String normalizedDroolsBasePath = getNormalizedRulePath(DROOLS_BASE_PATH);
		if (normalizedPath.startsWith(normalizedDroolsBasePath))
		{
			return normalizedPath.substring(normalizedDroolsBasePath.length()); // NOSONAR
		}
		return normalizedPath;
	}

	public static String getDeployedRulesModuleVersion(final RulesModule rulesModule)
	{
		checkArgument(Objects.nonNull(rulesModule), "Rules module shouldn't be null here");

		return rulesModule.getMvnVersion() + "." + rulesModule.getVersion();
	}

}
