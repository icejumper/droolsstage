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
package de.hybris.ruleengine.stage.drools;

import java.util.ArrayList;
import java.util.List;

import org.kie.api.runtime.rule.AgendaFilter;
import org.kie.api.runtime.rule.Match;


public class DefaultCompoundAgendaFilter implements CompoundAgendaFilter
{
	private List<AgendaFilter> agendaFilters;

	private boolean forceAllEvaluations = false;

	public DefaultCompoundAgendaFilter()
	{
		this.agendaFilters = new ArrayList<>();
	}

	@Override
	public boolean accept(final Match match)
	{
		boolean result = true;
		for (final AgendaFilter agendaFilter : getAgendaFilters())
		{
			result &= agendaFilter.accept(match);
			if (!result && !isForceAllEvaluations())
			{
				break;
			}
		}
		return result;
	}

	protected List<AgendaFilter> getAgendaFilters()
	{
		return agendaFilters;
	}

	protected boolean isForceAllEvaluations()
	{
		return forceAllEvaluations;
	}

	@Override
	public void setForceAllEvaluations(final boolean forceAllEvaluations)
	{
		this.forceAllEvaluations = forceAllEvaluations;
	}

	@Override
	public void setAgendaFilters(final List<AgendaFilter> agendaFilters)
	{
		this.agendaFilters = agendaFilters;
	}

}
