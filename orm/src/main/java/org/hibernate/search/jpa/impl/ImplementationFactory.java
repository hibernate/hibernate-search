/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa.impl;

import javax.persistence.EntityManager;

import org.hibernate.search.jpa.FullTextEntityManager;


/**
 * Creates concrete instances of FullTextEntityManager without exposing the underlying types.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 */
public final class ImplementationFactory {

	private ImplementationFactory() {
		//not meant to be instantiated
	}

	public static FullTextEntityManager createFullTextEntityManager(EntityManager em) {
		return new FullTextEntityManagerImpl( em );
	}

}
