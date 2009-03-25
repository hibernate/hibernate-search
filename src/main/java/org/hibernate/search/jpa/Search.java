// $Id$
package org.hibernate.search.jpa;

import javax.persistence.EntityManager;

import org.hibernate.search.jpa.impl.FullTextEntityManagerImpl;

/**
 * Helper class that should be used when building a FullTextEntityManager
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class Search {
	
	private Search() {
	}

	/**
	 * Build a full text capable EntityManager
	 * The underlying EM implementation has to be Hibernate EntityManager
	 */
	public static FullTextEntityManager getFullTextEntityManager(EntityManager em) {
		if ( em instanceof FullTextEntityManagerImpl ) {
			return (FullTextEntityManager) em;
		}
		else {
			return new FullTextEntityManagerImpl(em);
		}
	}

	/**
	 * @deprecated As of release 3.1.0, replaced by {@link #getFullTextEntityManager}
	 */
	@Deprecated
	public static FullTextEntityManager createFullTextEntityManager(EntityManager em) {
		return getFullTextEntityManager(em);
	}
}