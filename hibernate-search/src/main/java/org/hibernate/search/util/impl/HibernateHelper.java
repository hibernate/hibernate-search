/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.util.impl;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.search.backend.spi.Work;

/**
 * @author Emmanuel Bernard
 */
public final class HibernateHelper {
	private HibernateHelper() {
	}

	/**
	 * Get the real class type.
	 * In case of Hibernate proxies, return the entity type rather than the proxy's
	 */
	public static <T> Class<T> getClass(T entity) {
		return Hibernate.getClass( entity );
	}

	public static void initialize(Object entity) {
		Hibernate.initialize( entity );
	}

	public static boolean isInitialized(Object entity) {
		return Hibernate.isInitialized( entity );
	}

	public static <T> Class<T> getClassFromWork(Work<T> work) {
		return work.getEntityClass() != null ?
				work.getEntityClass() :
				getClass( work.getEntity() );
	}

	public static Object unproxy(Object value) {
		if ( value instanceof HibernateProxy ) {
			value = ( ( HibernateProxy ) value ).getHibernateLazyInitializer().getImplementation();
		}
		return value;
	}
}
