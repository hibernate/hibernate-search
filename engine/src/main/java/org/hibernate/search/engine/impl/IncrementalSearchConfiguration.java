/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.engine.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.impl.SearchFactoryState;

/**
 * @author Emmanuel Bernard
 */
public class IncrementalSearchConfiguration implements SearchConfiguration {
	private final List<Class<?>> classes;
	private final Map<String, Class<?>> classesByName = new HashMap<String, Class<?>>();
	private final SearchFactoryState searchFactoryState;
	private final Properties properties;
	private final ReflectionManager reflectionManager = new JavaReflectionManager();

	public IncrementalSearchConfiguration(List<Class<?>> classes, Properties properties, SearchFactoryState factoryState) {
		this.properties = properties;
		this.classes = classes;
		this.searchFactoryState = factoryState;
		for ( Class<?> entity : classes ) {
			classesByName.put( entity.getName(), entity );
		}
	}

	@Override
	public Iterator<Class<?>> getClassMappings() {
		return classes.iterator();
	}

	@Override
	public Class<?> getClassMapping(String name) {
		return classesByName.get( name );
	}

	@Override
	public String getProperty(String propertyName) {
		return properties.getProperty( propertyName );
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public ReflectionManager getReflectionManager() {
		return reflectionManager;
	}

	@Override
	public SearchMapping getProgrammaticMapping() {
		return searchFactoryState.getProgrammaticMapping();
	}

	@Override
	public Map<Class<? extends Service>, Object> getProvidedServices() {
		return Collections.emptyMap();
	}

	@Override
	public boolean isTransactionManagerExpected() {
		return searchFactoryState.isTransactionManagerExpected();
	}

	@Override
	public InstanceInitializer getInstanceInitializer() {
		return searchFactoryState.getInstanceInitializer();
	}

	@Override
	public boolean isIndexMetadataComplete() {
		return searchFactoryState.isIndexMetadataComplete();
	}

	@Override
	public boolean isDeleteByTermEnforced() {
		return searchFactoryState.isDeleteByTermEnforced();
	}

	@Override
	public boolean isIdProvidedImplicit() {
		return searchFactoryState.isIdProvidedImplicit();
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return searchFactoryState.getServiceManager().requestService( ClassLoaderService.class );
	}
}
