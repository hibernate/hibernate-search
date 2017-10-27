/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.orm.spi;

import java.util.Objects;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Simple helper to get a reference to the {@link SearchIntegrator} from
 * an Hibernate {@link SessionFactory} or a standard JPA
 * {@link EntityManagerFactory}.
 */
public final class SearchIntegratorHelper {

	private SearchIntegratorHelper() {
		//utility class: not to be constructed
	}

	/**
	 * Extract a {@link SearchIntegrator} from an active {@link SessionFactory}.
	 * @param sf the active SessionFactory
	 * @return the SearchIntegrator linked to the provided SessionFactory
	 */
	public static SearchIntegrator extractFromSessionFactory(final SessionFactory sf) {
		Objects.requireNonNull( sf );
		return org.hibernate.search.hcore.util.impl.ContextHelper.getSearchIntegratorBySF( sf );
	}

	/**
	 * Extract a {@link SearchIntegrator} from an active {@link EntityManagerFactory}.
	 * The provided EntityManagerFactory must be implemented by an Hibernate EntityManagerFactory.
	 * @param emf the active EntityManagerFactory
	 * @return the SearchIntegrator linked to the provided EntityManagerFactory
	 */
	public static SearchIntegrator extractFromEntityManagerFactory(final EntityManagerFactory emf) {
		Objects.requireNonNull( emf );
		try {
			SessionFactoryImpl sf = emf.unwrap( SessionFactoryImpl.class );
			return extractFromSessionFactory( sf );
		}
		catch (PersistenceException cce) {
			throw LoggerFactory.make( MethodHandles.lookup() ).incompatibleEntityManagerFactory( emf.getClass().toString() );
		}
	}

}
