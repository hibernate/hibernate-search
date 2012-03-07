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
package org.hibernate.search.cfg.spi;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.ServiceProvider;

/**
 * Provides configuration to Hibernate Search
 *
 * @author Navin Surtani  - navin@surtani.org
 * @author Emmanuel Bernard
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
	 * @param name
	 * @return corresponding class instance.
	 */

	Class<?> getClassMapping(String name);

	/**
	 * Gets a configuration property from its name
	 * or null if not present
	 *
	 * @param propertyName - as a String.
	 * @return the property as a String
	 */
	String getProperty(String propertyName);

	/**
	 * Gets properties as a java.util.Properties object.
	 *
	 * @return a java.util.Properties object.
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
	 * Provide service instances.
	 *
	 * Return the provided services (provider and instance at stake)
	 * These services are passed untouched by Hibernate Search via the
	 * {@link org.hibernate.search.spi.BuildContext#requestService(Class)} API
	 *
	 * Note that the lifecycle methods:
	 *  - {@link org.hibernate.search.spi.ServiceProvider#start(java.util.Properties)}
	 *  - {@link org.hibernate.search.spi.ServiceProvider#stop()}
	 * of the provider are *not* called.
	 *
	 * For services using the same ServiceProvider class, provided services have priority
	 * over managed services (ie the ones using the service locator pattern).
	 */
	Map<Class<? extends ServiceProvider<?>>, Object> getProvidedServices();

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

	InstanceInitializer getInstanceInitializer();
}
