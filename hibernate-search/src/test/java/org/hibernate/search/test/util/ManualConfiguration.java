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
package org.hibernate.search.test.util;

import java.util.Iterator;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

import org.hibernate.search.cfg.SearchConfiguration;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.search.spi.ServiceProvider;

/**
 * Manually defines the configuration
 * Classes and properties are the only implemented options at the moment
 *
 * @author Emmanuel Bernard
 */
public class ManualConfiguration implements SearchConfiguration {
	private final Map<String,Class<?>>  classes;
	private final Properties properties;
	private final HashMap<Class<? extends ServiceProvider<?>>, Object> providedServices;

	public ManualConfiguration() {
		classes = new HashMap<String,Class<?>>();
		properties = new Properties( );
		providedServices = new HashMap<Class<? extends ServiceProvider<?>>, Object>();
	}

	public ManualConfiguration addProperty(String key , String value) {
		properties.setProperty( key, value );
		return this;
	}

	public ManualConfiguration addClass(Class<?> indexed) {
		classes.put( indexed.getName(), indexed );
		return this;
	}

	public Iterator<Class<?>> getClassMappings() {
		return classes.values().iterator();
	}

	public Class<?> getClassMapping(String name) {
		return classes.get( name );
	}

	public String getProperty(String propertyName) {
		return properties.getProperty( propertyName );
	}

	public Properties getProperties() {
		return properties;
	}

	public ReflectionManager getReflectionManager() {
		return null;
	}

	public SearchMapping getProgrammaticMapping() {
		return null;
	}

	public Map<Class<? extends ServiceProvider<?>>, Object> getProvidedServices() {
		return providedServices;
	}
}
