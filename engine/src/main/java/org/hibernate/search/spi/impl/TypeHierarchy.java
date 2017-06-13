/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.spi.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Helper class which keeps track of all super classes and interfaces of known entities.
 */
public class TypeHierarchy {
	private static final Log log = LoggerFactory.make();

	private Map<Class<?>, Set<IndexedTypeIdentifier>> classToConfiguredClass;

	public TypeHierarchy() {
		classToConfiguredClass = new HashMap<Class<?>, Set<IndexedTypeIdentifier>>();
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
		Set<IndexedTypeIdentifier> classesSet = classToConfiguredClass.get( superclass );
		if ( classesSet == null ) {
			classesSet = new HashSet<IndexedTypeIdentifier>();
			classToConfiguredClass.put( superclass, classesSet );
		}
		classesSet.add( new PojoIndexedTypeIdentifier( indexedClass ) );
	}

	public IndexedTypeSet getConfiguredClasses(IndexedTypeSet types) {
		if ( types == null ) {
			return IndexedTypeSets.empty();
		}
		Set<IndexedTypeIdentifier> indexedClasses = new HashSet<IndexedTypeIdentifier>();
		for ( IndexedTypeIdentifier type : types ) {
			Class<?> clazz = type.getPojoType();
			Set<IndexedTypeIdentifier> set = classToConfiguredClass.get( clazz );
			if ( set != null ) {
				// at this point we don't have to care about including indexed subclasses of a indexed class
				// MultiClassesQueryLoader will take care of this later and optimise the queries
				indexedClasses.addAll( set );
			}
		}
		if ( log.isTraceEnabled() ) {
			log.tracef( "Targeted indexed classes for %s: %s", types, indexedClasses );
		}
		return IndexedTypeSets.fromIdentifiers( indexedClasses );
	}
}
