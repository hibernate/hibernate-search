/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
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
		if ( em instanceof FullTextEntityManager ) {
			return (FullTextEntityManager) em;
		}
		else {
			return new FullTextEntityManagerImpl( em );
		}
	}

}
