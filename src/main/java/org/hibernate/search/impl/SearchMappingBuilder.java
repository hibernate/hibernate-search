package org.hibernate.search.impl;

import java.lang.reflect.Method;

import org.hibernate.search.SearchException;
import org.hibernate.search.Environment;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.SearchConfiguration;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.util.LoggerFactory;
import org.hibernate.annotations.common.util.ReflectHelper;

import org.slf4j.Logger;

/**
 * package class extracting the SearchMappingFactory if needed
 */
class SearchMappingBuilder {
	private static final Logger LOG = LoggerFactory.make();

	private SearchMappingBuilder() {
	}

	/**
	 * This factory method takes a SearchConfiguration object
	 * and returns a SearchMapping object which defines
	 * the programmatic model for indexing entites and fields.
	 * 
	 * Throws SearchException:
	 * 1) No @Factory found
	 * 2) Multiple @Factory found
	 * 3) hibernate.search.model_mapping defines a class that cannot be found
	 * 4) Cannot invoke the @Factory method to get new instance of SearchMapping
	 * 
	 * @param SearchConfigruation
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
				makeMethodAccessibleIfRequired(method);
				mapping = getNewInstanceOfSearchMapping(clazz, method);
			}
		}
		validateMappingFactoryDefinition(count, clazz);
		return mapping;
	}

	private static SearchMapping getNewInstanceOfSearchMapping(Class<?> clazz, Method method) {
		SearchMapping mapping = null;
		try {
			LOG.debug("invoking factory method [ {}.{} ] to get search mapping instance", clazz.getName(), method.getName());
			Object instance = clazz.newInstance();
			mapping = (SearchMapping) method.invoke(instance);
		} catch (Exception e) {
			throw new SearchException("Unable to call the factory method: " + clazz.getName() + "." + method.getName(), e);
		}
		return mapping;
	}

	private static void makeMethodAccessibleIfRequired(Method method) {
		if ( !method.isAccessible() ) {
			method.setAccessible( true );
		}
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
				clazz = ReflectHelper.classForName( className, SearchMappingBuilder.class);
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
