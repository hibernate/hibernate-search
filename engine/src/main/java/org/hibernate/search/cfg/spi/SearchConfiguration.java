/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg.spi;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.spi.InstanceInitializer;

/**
 * Provides configuration to Hibernate Search. This is the entry point for bootstrapping Search.
 *
 * @author Navin Surtani  - navin@surtani.org
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public interface SearchConfiguration {
	/**
	 * Returns an iterator over the list of indexed classes
	 *
	 * @return iterator of indexed classes.
	 */
	Iterator<Class<?>> getClassMappings();

	/**
	 * Returns a {@link java.lang.Class} from a String parameter.
	 *
	 * @param name the class name as string
	 *
	 * @return corresponding class instance
	 */

	Class<?> getClassMapping(String name);

	/**
	 * Gets a configuration property from its name
	 * or null if not present
	 *
	 * @param propertyName - as a String.
	 *
	 * @return the property as a String
	 */
	String getProperty(String propertyName);

	/**
	 * Gets properties as a java.util.Properties object.
	 *
	 * @return a java.util.Properties object.
	 *
	 * @see java.util.Properties object
	 */
	Properties getProperties();

	/**
	 * Returns a reflection manager if already available in the environment
	 * null otherwise
	 *
	 * @return ReflectionManager
	 */
	ReflectionManager getReflectionManager();

	/**
	 * @return the programmatic configuration or {@code null}
	 */
	SearchMapping getProgrammaticMapping();

	/**
	 * Return the provided services.
	 *
	 * These services are passed untouched by Hibernate Search. Provided services have priority
	 * over managed services (ie the ones using the service locator pattern).
	 * <p>
	 * Provided services are also not allowed to implement {@link org.hibernate.search.engine.service.spi.Startable} or
	 * {@link org.hibernate.search.engine.service.spi.Stoppable}. An exception is thrown in this case.
	 * </p>
	 *
	 * @return a map of service roles to service instances
	 */
	Map<Class<? extends Service>, Object> getProvidedServices();

	/**
	 * By default Hibernate Search expects to execute in the context of a transaction,
	 * and will log warning when certain operations are executed out of such a scope.
	 *
	 * @return when returning {@code false} Search will avoid logging such warnings.
	 */
	boolean isTransactionManagerExpected();

	/**
	 * @return {@code true} if it is safe to assume that the information we have about
	 * index metadata is accurate. This should be set to false for example if the index
	 * could contain Documents related to types not known to this SearchFactory instance.
	 */
	boolean isIndexMetadataComplete();

	/**
	 * @return {@code true} if regardless of {@code isIndexMetadataComplete} and the number
	 * of types present in the index it is safe to delete by term given that the underlying
	 * store guarantees uniqueness of ids
	 */
	boolean isDeleteByTermEnforced();

	/**
	 * Returns the initializer to be used to initialize potentially lazy entities or collections.
	 *
	 * @return the initializer to be used to initialize potentially lazy entities or collections.
	 */
	InstanceInitializer getInstanceInitializer();

	/**
	 * @return {@code true} if we should treat indexed entities as implicitly annotated
	 * with a {@link org.hibernate.search.annotations.ProvidedId}, if no other Id is specified.
	 */
	boolean isIdProvidedImplicit();

	/**
	 * @return Returns a classloader service for this configuration of Search. Access to the service is via the
	 * {@link org.hibernate.search.engine.service.spi.ServiceManager}
	 */
	ClassLoaderService getClassLoaderService();
}
