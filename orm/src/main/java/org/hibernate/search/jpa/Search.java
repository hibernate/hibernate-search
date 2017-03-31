/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Helper class that should be used when building a FullTextEntityManager
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class Search {

	private static final Log log = LoggerFactory.make();

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
	 */
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
		Object delegate = em.getDelegate();
		if ( delegate == null ) {
			throw new SearchException(
					"Trying to use Hibernate Search without an Hibernate EntityManager (no delegate)"
			);
		}
		else if ( Session.class.isAssignableFrom( delegate.getClass() ) ) {
			return (Session) delegate;
		}
		else if ( EntityManager.class.isAssignableFrom( delegate.getClass() ) ) {
			//Some app servers wrap the EM twice
			delegate = ( (EntityManager) delegate ).getDelegate();
			if ( delegate == null ) {
				throw new SearchException(
						"Trying to use Hibernate Search without an Hibernate EntityManager (no delegate)"
				);
			}
			else if ( Session.class.isAssignableFrom( delegate.getClass() ) ) {
				return (Session) delegate;
			}
			else {
				throw new SearchException(
						"Trying to use Hibernate Search without an Hibernate EntityManager: " + delegate.getClass()
				);
			}
		}
		else {
			throw new SearchException(
					"Trying to use Hibernate Search without an Hibernate EntityManager: " + delegate.getClass()
			);
		}
	}

}
