/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.event.impl;

import org.hibernate.search.mapper.orm.mapping.impl.HibernateSearchContextService;

/**
 * Internal contract to allow switching different strategies
 * to access some configured state from the EventListener, of
 * particular use during (deferred) initialization of the
 * Search engine.
 */
public interface EventsHibernateSearchState {

	/**
	 * @return the initialized Hibernate Search context
	 */
	HibernateSearchContextService getHibernateSearchContext();

}
