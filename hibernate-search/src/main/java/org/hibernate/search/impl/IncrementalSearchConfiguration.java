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
import org.hibernate.search.cfg.SearchConfiguration;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.spi.ServiceProvider;

/**
 * @author Emmanuel Bernard
 */
public class IncrementalSearchConfiguration implements SearchConfiguration {
	private final ReflectionManager reflectionManager = new JavaReflectionManager();
	private final List<Class<?>> classes;
	private final Map<String, Class<?>> classesByName = new HashMap<String, Class<?>>();
	private Properties properties;

	public IncrementalSearchConfiguration(List<Class<?>> classes, Properties properties) {
		this.properties = properties;
		this.classes = classes;
		for ( Class<?> entity : classes ) {
			classesByName.put( entity.getName(), entity );
		}
	}
	public Iterator<Class<?>> getClassMappings() {
		return classes.iterator();
	}

	public Class<?> getClassMapping(String name) {
		return classesByName.get( name );
	}

	public String getProperty(String propertyName) {
		return properties.getProperty(propertyName );
	}

	public Properties getProperties() {
		return properties;
	}

	public ReflectionManager getReflectionManager() {
		return reflectionManager;
	}

	public SearchMapping getProgrammaticMapping() {
		return null;
	}

	public Map<Class<? extends ServiceProvider<?>>, Object> getProvidedServices() {
		return Collections.emptyMap();
	}
}
