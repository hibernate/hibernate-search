package org.hibernate.search.util;

import org.hibernate.Hibernate;
import org.hibernate.search.backend.Work;

/**
 * @author Emmanuel Bernard
 */
public final class HibernateHelper {
	private HibernateHelper() {};

	/**
	 * Get the real class type.
	 * In case of Hibernate proxies, return the entity type rather than the proxy's
	 */
	public static <T> Class<T> getClass(T entity) {
		@SuppressWarnings("unchecked")
		final Class<T> type = Hibernate.getClass( entity );
		return type;
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
}
