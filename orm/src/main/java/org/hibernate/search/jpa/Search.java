/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa;

import javax.persistence.EntityManager;

import org.hibernate.search.jpa.impl.ImplementationFactory;

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
		if ( em instanceof FullTextEntityManager ) {
			return (FullTextEntityManager) em;
		}
		else {
			return ImplementationFactory.createFullTextEntityManager( em );
		}
	}

}
