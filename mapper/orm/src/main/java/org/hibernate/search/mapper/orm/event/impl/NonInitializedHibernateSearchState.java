/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.event.impl;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.orm.impl.HibernateSearchContextService;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * The implementation of EventsHibernateSearchState which the
 * FullTextIndexEventListener will be pointing to before
 * we start initialization of Hibernate Search.
 * This should never be needed, but the parallel initialization
 * of multiple frameworks is tricky to predict:
 * if it happens at least we provide a meaningful error
 * message rather than a null pointer.
 *
 * @author Sanne Grinovero
 */
// Implementation note: do not change this into a singleton.
// The hope is that after boot time there will no longer be any
// instances of this type around, allowing the JVM to avoid
// megamorphic invocations, as these methods are on the hot path.
final class NonInitializedHibernateSearchState implements EventsHibernateSearchState, Serializable {

	@Override
	public HibernateSearchContextService getHibernateSearchContext() {
		throw notInitialized();
	}

	private SearchException notInitialized() {
		// do not make a static field as we want this class to be disposable.
		// we're unlikely to ever need this logger anyway!
		return LoggerFactory.make( Log.class, MethodHandles.lookup() ).hibernateSearchNotInitialized();
	}

}
