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

package org.hibernate.search.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.ServiceProvider;
import org.hibernate.search.spi.internals.SearchFactoryState;

/**
 * @author Emmanuel Bernard
 */
public class IncrementalSearchConfiguration implements SearchConfiguration {
	private final List<Class<?>> classes;
	private final Map<String, Class<?>> classesByName = new HashMap<String, Class<?>>();
	private final SearchFactoryState state;
	private final Properties properties;
	private final ReflectionManager reflectionManager = new JavaReflectionManager();

	public IncrementalSearchConfiguration(List<Class<?>> classes, Properties properties, SearchFactoryState factoryState) {
		this.properties = properties;
		this.classes = classes;
		this.state = factoryState;
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
		return state.getProgrammaticMapping();
	}

	@Override
	public Map<Class<? extends ServiceProvider<?>>, Object> getProvidedServices() {
		return Collections.emptyMap();
	}

	@Override
	public boolean isTransactionManagerExpected() {
		return state.isTransactionManagerExpected();
	}

	@Override
	public InstanceInitializer getInstanceInitializer() {
		return state.getInstanceInitializer();
	}

	@Override
	public boolean isIndexMetadataComplete() {
		return state.isIndexMetadataComplete();
	}

	@Override
	public boolean isIdProvidedImplicit() {
		return state.isIdProvidedImplicit();
	}

	@Override
	public IndexManagerFactory getIndexManagerFactory() {
		return state.getIndexManagerFactory();
	}
}
