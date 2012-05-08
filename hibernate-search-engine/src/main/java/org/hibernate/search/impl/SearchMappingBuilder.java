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

import java.lang.reflect.Method;

import org.hibernate.search.SearchException;
import org.hibernate.search.Environment;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * package class extracting the SearchMappingFactory if needed
 */
public class SearchMappingBuilder {
	private static final Log LOG = LoggerFactory.make();

	private SearchMappingBuilder() {
	}

	/**
	 * This factory method takes a SearchConfiguration object
	 * and returns a SearchMapping object which defines
	 * the programmatic model for indexing entities and fields.
	 * 
	 * Throws SearchException:
	 * 1) No @Factory found
	 * 2) Multiple @Factory found
	 * 3) hibernate.search.model_mapping defines a class that cannot be found
	 * 4) Cannot invoke the @Factory method to get new instance of SearchMapping
	 * 
	 * @param cfg the search configuration
	 * @return SearchMapping
	 */
	public static SearchMapping getSearchMapping(SearchConfiguration cfg) {

		//try SearchConfiguration object first and then properties
		Object modelMappingProperty = cfg.getProgrammaticMapping();
		if ( modelMappingProperty == null) {
			modelMappingProperty = cfg.getProperties().get( Environment.MODEL_MAPPING );
		}

		if ( modelMappingProperty == null) {
			return null;
		}
		SearchMapping mapping = null;
		Object programmaticConfig = modelMappingProperty;
		if (programmaticConfig instanceof SearchMapping) {
			mapping = (SearchMapping) programmaticConfig;
			return mapping;
		}
		Class<?> clazz = getProgrammaticMappingClass(programmaticConfig);
		Method[] methods = clazz.getDeclaredMethods();
		int count = 0;
		for (Method method : methods) {
			if (method.isAnnotationPresent(Factory.class)) {
				count++;
				ReflectionHelper.setAccessible( method );
				mapping = getNewInstanceOfSearchMapping(clazz, method);
			}
		}
		validateMappingFactoryDefinition(count, clazz);
		return mapping;
	}

	private static SearchMapping getNewInstanceOfSearchMapping(Class<?> clazz, Method method) {
		SearchMapping mapping = null;
		try {
			LOG.debugf("invoking factory method [ %s.%s ] to get search mapping instance", clazz.getName(), method.getName());
			Object instance = clazz.newInstance();
			mapping = (SearchMapping) method.invoke(instance);
		} catch (Exception e) {
			throw new SearchException("Unable to call the factory method: " + clazz.getName() + "." + method.getName(), e);
		}
		return mapping;
	}

	private static void validateMappingFactoryDefinition(int count, Class<?> factory) {
		if (count == 0) {
			throw new SearchException("No @Factory method defined for building programmatic api on " + factory);
		}
		if (count > 1) {
			throw new SearchException("Multiple @Factory methods defined. Only one factory method required. " + factory);
		}
	}

	private static Class<?> getProgrammaticMappingClass(Object programmaticConfig) {
		Class<?> clazz = null;
		if (programmaticConfig instanceof String) {
			final String className = ( String ) programmaticConfig;
			try {
				clazz = ClassLoaderHelper.classForName( className, SearchMappingBuilder.class.getClassLoader() );
			} catch (ClassNotFoundException e) {
				throw new SearchException("Unable to find " + Environment.MODEL_MAPPING + "=" + className, e);
			}
		} else if (programmaticConfig instanceof Class){
			clazz = (Class<?>) programmaticConfig;
		}
		else {
			throw new SearchException(Environment.MODEL_MAPPING + " is of an unknown type: " + programmaticConfig.getClass() );
		}
		if (clazz == null) {
			throw new SearchException("No programmatic factory defined");
		}
		return clazz;
	}
}
