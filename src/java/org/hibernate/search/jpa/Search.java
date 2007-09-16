// $Id$
package org.hibernate.search.jpa;

import javax.persistence.EntityManager;

import org.hibernate.search.jpa.impl.FullTextEntityManagerImpl;

/**
 * Helper class that should be used when building a FullTextEntityManager
 *
 * @author Emmanuel Bernard
 */
public final class Search {
	private Search() {
	}

	/**
	 * Build a full text capable EntityManager
	 * The underlying EM implementation has to be Hibernate EntityManager
	 */
	public static FullTextEntityManager createFullTextEntityManager(EntityManager em) {
		if ( em instanceof FullTextEntityManagerImpl ) {
			return (FullTextEntityManager) em;
		}
		else {
			return new FullTextEntityManagerImpl(em);
		}
	}
}