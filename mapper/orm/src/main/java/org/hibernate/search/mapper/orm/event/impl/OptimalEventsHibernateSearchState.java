/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.event.impl;

import org.hibernate.search.util.common.impl.Contracts;

/**
 * The implementation of EventsHibernateSearchState used at runtime,
 * after initialization of the ExtendedSearchIntegrator has been
 * performed.
 *
 * @author Sanne Grinovero
 */
final class OptimalEventsHibernateSearchState implements EventsHibernateSearchState {

	private final HibernateOrmListenerContextProvider contextProvider;

	OptimalEventsHibernateSearchState(HibernateOrmListenerContextProvider contextProvider) {
		Contracts.assertNotNull( contextProvider, "contextProvider" );
		this.contextProvider = contextProvider;
	}

	@Override
	public HibernateOrmListenerContextProvider getContextProvider() {
		return contextProvider;
	}

}
