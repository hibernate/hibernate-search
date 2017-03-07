/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.spi.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Helper class which keeps track of all super classes and interfaces of known entities.
 */
public class TypeHierarchy {
	private static final Log log = LoggerFactory.make();

	private Map<Class<?>, Set<Class<?>>> classToConfiguredClass;

	public TypeHierarchy() {
		classToConfiguredClass = new HashMap<Class<?>, Set<Class<?>>>();
	}

	public void addConfiguredClass(Class<?> configuredClass) {
		addClass( configuredClass, configuredClass );
		Class<?> superClass = configuredClass.getSuperclass();
		while ( superClass != null ) {
			addClass( superClass, configuredClass );
			superClass = superClass.getSuperclass();
		}
		for ( Class<?> clazz : configuredClass.getInterfaces() ) {
			addClass( clazz, configuredClass );
		}
	}

	private void addClass(Class<?> superclass, Class<?> indexedClass) {
		Set<Class<?>> classesSet = classToConfiguredClass.get( superclass );
		if ( classesSet == null ) {
			classesSet = new HashSet<Class<?>>();
			classToConfiguredClass.put( superclass, classesSet );
		}
		classesSet.add( indexedClass );
	}

	public Set<Class<?>> getConfiguredClasses(Class<?>[] classes) {
		if ( classes == null ) {
			return Collections.<Class<?>>emptySet();
		}
		Set<Class<?>> indexedClasses = new HashSet<Class<?>>();
		for ( Class<?> clazz : classes ) {
			Set<Class<?>> set = classToConfiguredClass.get( clazz );
			if ( set != null ) {
				// at this point we don't have to care about including indexed subclasses of a indexed class
				// MultiClassesQueryLoader will take care of this later and optimise the queries
				indexedClasses.addAll( set );
			}
		}
		if ( log.isTraceEnabled() ) {
			log.tracef( "Targeted indexed classes for %s: %s", Arrays.toString( classes ), indexedClasses );
		}
		return indexedClasses;
	}
}
