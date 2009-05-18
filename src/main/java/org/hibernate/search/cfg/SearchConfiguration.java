// $Id$
package org.hibernate.search.cfg;

import java.util.Iterator;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;

/**
 * Provides configuration to Hibernate Search
 *
 * @author Navin Surtani  - navin@surtani.org
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
	 * @return An iterator of Classes.
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
	 * returns the programmatic configuration or null
	 * //TODO remove hard dep with solr classes
	 */
	SearchMapping getProgrammaticMapping();
}
