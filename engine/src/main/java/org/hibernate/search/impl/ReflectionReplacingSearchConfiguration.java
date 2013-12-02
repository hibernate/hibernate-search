/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.ServiceProvider;

/**
 * Wraps another SearchConfiguration to override it's ReflectionManager
 *
 * @author Sanne Grinovero
 * @since 4.1
 */
public final class ReflectionReplacingSearchConfiguration implements SearchConfiguration {

	private final ReflectionManager reflectionManager;
	private final SearchConfiguration cfg;

	/**
	 * Create a new SearchConfiguration which returns the same values as the provided SearchConfiguration
	 * instance, with the exception of {@link #getReflectionManager()} which will return the constructor
	 * defined ReflectionManager.
	 *
	 * @param reflectionManager the current reflection manager
	 * @param cfg the search configuration
	 */
	public ReflectionReplacingSearchConfiguration(ReflectionManager reflectionManager, SearchConfiguration cfg) {
		this.reflectionManager = reflectionManager;
		this.cfg = cfg;
	}

	@Override
	public Iterator<Class<?>> getClassMappings() {
		return cfg.getClassMappings();
	}

	@Override
	public Class<?> getClassMapping(String name) {
		return cfg.getClassMapping( name );
	}

	@Override
	public String getProperty(String propertyName) {
		return cfg.getProperty( propertyName );
	}

	@Override
	public Properties getProperties() {
		return cfg.getProperties();
	}

	@Override
	public ReflectionManager getReflectionManager() {
		return reflectionManager;
	}

	@Override
	public SearchMapping getProgrammaticMapping() {
		return cfg.getProgrammaticMapping();
	}

	@Override
	public Map<Class<? extends ServiceProvider<?>>, Object> getProvidedServices() {
		return cfg.getProvidedServices();
	}

	@Override
	public boolean isTransactionManagerExpected() {
		return cfg.isTransactionManagerExpected();
	}

	@Override
	public InstanceInitializer getInstanceInitializer() {
		return cfg.getInstanceInitializer();
	}

	@Override
	public boolean isIndexMetadataComplete() {
		return cfg.isIndexMetadataComplete();
	}

	@Override
	public boolean isIdProvidedImplicit() {
		return cfg.isIdProvidedImplicit();
	}

	@Override
	public IndexManagerFactory getIndexManagerFactory() {
		return cfg.getIndexManagerFactory();
	}

}
