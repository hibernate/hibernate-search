/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa;

import java.lang.invoke.MethodHandles;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.hibernate.Session;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Helper class that should be used when building a FullTextEntityManager
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @deprecated Use {@link org.hibernate.search.mapper.orm.Search} instead.
 */
@Deprecated
public final class Search {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private Search() {
	}

	/**
	 * Build a full text capable EntityManager
	 * The underlying EM implementation has to be Hibernate EntityManager
	 * The created instance depends on the passed Session: closing either of them will
	 * close both instances. They both share the same persistence context.
	 *
	 * @param em the entityManager instance to use
	 * @return a FullTextEntityManager, wrapping the passed EntityManager
	 * @throws IllegalArgumentException if passed null
	 * @deprecated Use {@link org.hibernate.search.mapper.orm.Search#session(EntityManager)} instead.
	 */
	@Deprecated
	public static FullTextEntityManager getFullTextEntityManager(EntityManager em) {
		if ( em == null ) {
			throw log.getNullEntityManagerPassedToFullEntityManagerCreationException();
		}
		else if ( em instanceof FullTextEntityManager ) {
			return (FullTextEntityManager) em;
		}
		else {
			return org.hibernate.search.Search.getFullTextSession( getSession( em ) );
		}
	}

	private static Session getSession(EntityManager em) {
		try {
			return em.unwrap( Session.class );
		}
		catch (PersistenceException e) {
			throw new SearchException(
					"Trying to use Hibernate Search with a non-Hibernate EntityManager", e
			);
		}
	}

}
