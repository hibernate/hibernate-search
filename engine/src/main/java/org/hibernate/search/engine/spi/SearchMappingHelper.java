/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spi;

import java.lang.reflect.Method;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Helper to extract the programmatic SearchMapping from a configuration object
 */
public class SearchMappingHelper {

	private static final Log LOG = LoggerFactory.make();

	private SearchMappingHelper() {
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
	public static SearchMapping extractSearchMapping(SearchConfiguration cfg) {
		// try SearchConfiguration object first and then properties
		Object modelMappingProperty = cfg.getProgrammaticMapping();
		if ( modelMappingProperty == null ) {
			modelMappingProperty = cfg.getProperties().get( Environment.MODEL_MAPPING );
		}

		if ( modelMappingProperty == null ) {
			return null;
		}
		SearchMapping mapping = null;
		Object programmaticConfig = modelMappingProperty;
		if ( programmaticConfig instanceof SearchMapping ) {
			mapping = (SearchMapping) programmaticConfig;
			return mapping;
		}
		// TODO - It would be nice to get the class loader service from the service manager, instead of directly from
		// the SearchConfiguration. However, with the current bootstrapping approach that is not possible.
		// Maybe this can be addressed with a refactoring of the bootstrap process. (HF)
		Class<?> clazz = getProgrammaticMappingClass( programmaticConfig, cfg.getClassLoaderService() );
		Method[] methods = clazz.getDeclaredMethods();
		int count = 0;
		for ( Method method : methods ) {
			if ( method.isAnnotationPresent( Factory.class ) ) {
				count++;
				ReflectionHelper.setAccessible( method );
				mapping = getNewInstanceOfSearchMapping( clazz, method );
			}
		}
		validateMappingFactoryDefinition( count, clazz );
		return mapping;
	}

	private static SearchMapping getNewInstanceOfSearchMapping(Class<?> clazz, Method method) {
		SearchMapping mapping;
		try {
			LOG.debugf( "invoking factory method [ %s.%s ] to get search mapping instance", clazz.getName(), method.getName() );
			Object instance = clazz.newInstance();
			mapping = (SearchMapping) method.invoke( instance );
		}
		catch (Exception e) {
			throw new SearchException( "Unable to call the factory method: " + clazz.getName() + "." + method.getName(), e );
		}
		return mapping;
	}

	private static void validateMappingFactoryDefinition(int count, Class<?> factory) {
		if ( count == 0 ) {
			throw new SearchException( "No @Factory method defined for building programmatic api on " + factory );
		}
		if ( count > 1 ) {
			throw new SearchException( "Multiple @Factory methods defined. Only one factory method required. " + factory );
		}
	}

	private static Class<?> getProgrammaticMappingClass(Object programmaticConfig, ClassLoaderService classLoaderService) {
		Class<?> clazz;
		if ( programmaticConfig instanceof String ) {
			final String className = (String) programmaticConfig;
			try {
				clazz = classLoaderService.classForName( className );
			}
			catch (ClassLoadingException e) {
				throw new SearchException( "Unable to find " + Environment.MODEL_MAPPING + "=" + className, e );
			}
		}
		else if ( programmaticConfig instanceof Class ) {
			clazz = (Class<?>) programmaticConfig;
		}
		else {
			throw new SearchException( Environment.MODEL_MAPPING + " is of an unknown type: " + programmaticConfig.getClass() );
		}
		if ( clazz == null ) {
			throw new SearchException( "No programmatic factory defined" );
		}
		return clazz;
	}
}
