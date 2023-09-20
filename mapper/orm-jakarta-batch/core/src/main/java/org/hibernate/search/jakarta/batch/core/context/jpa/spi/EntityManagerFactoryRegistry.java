/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jakarta.batch.core.context.jpa.spi;

import jakarta.persistence.EntityManagerFactory;

/**
 * An abstract contract allowing to retrieve an entity manager factory.
 *
 * @author Yoann Rodiere
 */
public interface EntityManagerFactoryRegistry {

	/**
	 * @return The default {@link EntityManagerFactory}, if there is one.
	 */
	EntityManagerFactory useDefault();

	/**
	 * Retrieve a factory using the default (implementation-dependent) scope.
	 *
	 * @param reference The reference allowing to identify the factory uniquely. Must be non-null and non-empty.
	 * @return The {@link EntityManagerFactory} for the given reference string.
	 */
	EntityManagerFactory get(String reference);

	/**
	 * Retrieve a factory using the given scope.
	 *
	 * @param namespace The namespace of the reference; accepted namespaces are implementation-dependent.
	 * Must be non-null and non-empty.
	 * For instance an implementation could accept the namespace 'persistence-unit-name', meaning
	 * that the reference will be interpreted as a persistence unit name.
	 * @param reference The reference allowing to identify the factory uniquely. Must be non-null and non-empty.
	 * @return The {@link EntityManagerFactory} for the given reference string.
	 */
	EntityManagerFactory get(String namespace, String reference);

}
