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

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import de.hybris.ruleengine.stage.DroolsSessionType;
import de.hybris.ruleengine.stage.init.RuleEngineContext;
import de.hybris.ruleengine.stage.init.RuleEngineKieModuleSwapper;
import de.hybris.ruleengine.stage.init.RuleEvaluationContext;
import de.hybris.ruleengine.stage.init.StatelessKieSessionHelper;
import de.hybris.ruleengine.stage.model.DroolsKieSession;
import de.hybris.ruleengine.stage.model.RulesBase;
import de.hybris.ruleengine.stage.model.RulesModule;

import org.drools.compiler.kproject.ReleaseIdImpl;
import org.drools.core.event.DebugAgendaEventListener;
import org.drools.core.event.DebugRuleRuntimeEventListener;
import org.kie.api.builder.ReleaseId;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.api.runtime.rule.AgendaFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;


/**
 * Default implementation of {@link StatelessKieSessionHelper}
 */
@Component
public class DefaultStatelessKieSessionHelper implements StatelessKieSessionHelper
{

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStatelessKieSessionHelper.class);
	private static final String KIE_MODULE_DUMMY_VERSION = "DUMMY_VERSION";
	private static final String KIE_MODULE_DUMMY_GROUPID = "DUMMY_GROUP";
	private static final String KIE_MODULE_DUMMY_ARTIFACTID = "DUMMY_ARTIFACT";

	@Autowired
	private RuleEngineKieModuleSwapper ruleEngineKieModuleSwapper;

	@Override
	public StatelessKieSession initializeSession(final RuleEvaluationContext context, final KieContainer kieContainer)
	{
		final RuleEngineContext ruleEngineContext = validateRuleEvaluationContext(context);
		final StatelessKieSession session = kieContainer.newStatelessKieSession(ruleEngineContext.getKieSession().getName());

		if (nonNull(context.getGlobals()))
		{
			context.getGlobals().forEach(session::setGlobal);
		}
		registerSessionListeners(context, session);
		return session;
	}

	@Override
	public ReleaseId getDeployedKieModuleReleaseId(final RuleEvaluationContext context)
	{
		final RuleEngineContext ruleEngineContext = validateRuleEvaluationContext(context);
		final DroolsKieSession kieSession = ruleEngineContext.getKieSession();
		final RulesBase kieBase = kieSession.getKieBase();
		final RulesModule kieModule = kieBase.getKieModule();
		return ruleEngineKieModuleSwapper
				.getDeployedReleaseId(kieModule, null)
				.orElse(getDummyReleaseId(kieModule));
	}

	protected ReleaseId getDummyReleaseId(final RulesModule module)
	{
		final String groupId = module.getMvnGroupId();
		final String artifactId = module.getMvnArtifactId();

		return new ReleaseIdImpl(nonNull(groupId) ? groupId : KIE_MODULE_DUMMY_GROUPID,
				nonNull(artifactId) ? artifactId : KIE_MODULE_DUMMY_ARTIFACTID, KIE_MODULE_DUMMY_VERSION);
	}

	protected void registerSessionListeners(final RuleEvaluationContext context, final StatelessKieSession session)
	{
		if (isNotEmpty(context.getEventListeners()))
		{
			for (final Object listener : context.getEventListeners())
			{
				if (listener instanceof AgendaEventListener)
				{
					session.addEventListener((AgendaEventListener) listener);
				}
				else if (listener instanceof RuleRuntimeEventListener)
				{
					session.addEventListener((RuleRuntimeEventListener) listener);
				}
				else if (listener instanceof ProcessEventListener)
				{
					session.addEventListener((ProcessEventListener) listener);
				}
				else
				{
					throw new IllegalArgumentException("context.eventListeners attribute must only contain instances of the types "
							+ "org.kie.api.event.rule.AgendaEventListener, org.kie.api.event.process.ProcessEventListener or "
							+ "org.kie.api.event.rule.RuleRuntimeEventListener");
				}
			}
		}
		if (LOGGER.isInfoEnabled())
		{
			session.addEventListener(new DebugRuleRuntimeEventListener());
			session.addEventListener(new DebugAgendaEventListener());
		}
	}

	protected RuleEngineContext validateRuleEvaluationContext(final RuleEvaluationContext context)
	{
		final RuleEngineContext ruleEngineContext = context.getRuleEngineContext();
		Preconditions.checkNotNull(ruleEngineContext, "rule engine context must not be null");
		if (!isSessionStateless(ruleEngineContext))
		{
			throw new IllegalArgumentException("This version of the hybris rule engine does not support drools stateful sessions.");
		}

		if (nonNull(context.getFilter()) && !(context.getFilter() instanceof AgendaFilter))
		{
			throw new IllegalArgumentException("context.filter attribute must be of type org.kie.api.runtime.rule.AgendaFilter");
		}
		return ruleEngineContext;
	}

	/**
	 * Determine if the current RuleEngineContext's KieSession is Stateless.
	 *
	 * @param ruleEngineContext
	 * 		the rule engine context
	 * @return true if stateless, false otherwise
	 */
	protected boolean isSessionStateless(final RuleEngineContext ruleEngineContext)
	{
		return DroolsSessionType.STATELESS.equals(ruleEngineContext.getKieSession().getSessionType());
	}
	
}
