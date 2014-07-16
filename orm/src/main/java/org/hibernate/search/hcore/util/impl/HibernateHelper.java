/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.hcore.util.impl;

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

	public static Class<?> getClassFromWork(Work work) {
		return work.getEntityClass() != null ?
				work.getEntityClass() :
				getClass( work.getEntity() );
	}

	public static Object unproxy(Object value) {
		if ( value instanceof HibernateProxy ) {
			value = ( (HibernateProxy) value ).getHibernateLazyInitializer().getImplementation();
		}
		return value;
	}
}
