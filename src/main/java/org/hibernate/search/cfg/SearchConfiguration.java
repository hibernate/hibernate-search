/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
