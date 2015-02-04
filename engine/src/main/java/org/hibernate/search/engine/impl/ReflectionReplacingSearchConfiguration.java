/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.spi.InstanceInitializer;

/**
 * Wraps another SearchConfiguration to override it's ReflectionManager
 *
 * @author Sanne Grinovero
 * @since 4.1
 */
public final class ReflectionReplacingSearchConfiguration implements SearchConfiguration {

	private final ReflectionManager reflectionManager;
	private final SearchConfiguration searchConfiguration;

	/**
	 * Create a new SearchConfiguration which returns the same values as the provided SearchConfiguration
	 * instance, with the exception of {@link #getReflectionManager()} which will return the constructor
	 * defined ReflectionManager.
	 *
	 * @param reflectionManager the current reflection manager
	 * @param searchConfiguration the search configuration
	 */
	public ReflectionReplacingSearchConfiguration(ReflectionManager reflectionManager, SearchConfiguration searchConfiguration) {
		this.reflectionManager = reflectionManager;
		this.searchConfiguration = searchConfiguration;
	}

	@Override
	public Iterator<Class<?>> getClassMappings() {
		return searchConfiguration.getClassMappings();
	}

	@Override
	public Class<?> getClassMapping(String name) {
		return searchConfiguration.getClassMapping( name );
	}

	@Override
	public String getProperty(String propertyName) {
		return searchConfiguration.getProperty( propertyName );
	}

	@Override
	public Properties getProperties() {
		return searchConfiguration.getProperties();
	}

	@Override
	public ReflectionManager getReflectionManager() {
		return reflectionManager;
	}

	@Override
	public SearchMapping getProgrammaticMapping() {
		return searchConfiguration.getProgrammaticMapping();
	}

	@Override
	public Map<Class<? extends Service>, Object> getProvidedServices() {
		return searchConfiguration.getProvidedServices();
	}

	@Override
	public boolean isTransactionManagerExpected() {
		return searchConfiguration.isTransactionManagerExpected();
	}

	@Override
	public InstanceInitializer getInstanceInitializer() {
		return searchConfiguration.getInstanceInitializer();
	}

	@Override
	public boolean isIndexMetadataComplete() {
		return searchConfiguration.isIndexMetadataComplete();
	}

	@Override
	public boolean isDeleteByTermEnforced() {
		return searchConfiguration.isDeleteByTermEnforced();
	}

	@Override
	public boolean isIdProvidedImplicit() {
		return searchConfiguration.isIdProvidedImplicit();
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return searchConfiguration.getClassLoaderService();
	}
}
