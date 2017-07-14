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

import java.util.List;

import org.kie.api.runtime.rule.AgendaFilter;
import org.kie.api.runtime.rule.Match;



/**
 * Provides a compound AgendaFilter which calls the {@link #accept(Match)} method for its list of agenda filters (set
 * via {@link #setAgendaFilters(List)}. It returns true only if all the contained agenda filters return true. The
 * evaluation of the contained AgendaFilters is stopped as soon as the first agenda filter returns false (unless the
 * {@link #setForceAllEvaluations} field is set to true which forces the evaluation of all agenda filters.
 */
public interface CompoundAgendaFilter extends AgendaFilter
{

	/**
	 * if set to true the compound agenda filter will evaluate all registered agenda filters via their
	 * {@link #accept(Match)} method (as opposed to stop evaluation when the first agenda filter returns false)
	 */
	public void setForceAllEvaluations(boolean forceAllEvaluations); // NOSONAR

	/**
	 * set the given agenda filters for this compound agenda filter.
	 */
	public void setAgendaFilters(List<AgendaFilter> agendaFilters);  // NOSONAR

}
