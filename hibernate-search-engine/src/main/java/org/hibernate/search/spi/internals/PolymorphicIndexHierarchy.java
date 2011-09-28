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

package org.hibernate.search.spi.internals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Helper class which keeps track of all super classes and interfaces of the indexed entities.
 */
//FIXME make it immutable (builder pattern)
public class PolymorphicIndexHierarchy {
	private static final Log log = LoggerFactory.make();

	private Map<Class<?>, Set<Class<?>>> classToIndexedClass;

	public PolymorphicIndexHierarchy() {
		classToIndexedClass = new HashMap<Class<?>, Set<Class<?>>>();
	}

	public void addIndexedClass(Class<?> indexedClass) {
		addClass( indexedClass, indexedClass );
		Class<?> superClass = indexedClass.getSuperclass();
		while ( superClass != null ) {
			addClass( superClass, indexedClass );
			superClass = superClass.getSuperclass();
		}
		for ( Class<?> clazz : indexedClass.getInterfaces() ) {
			addClass( clazz, indexedClass );
		}
	}

	private void addClass(Class<?> superclass, Class<?> indexedClass) {
		Set<Class<?>> classesSet = classToIndexedClass.get( superclass );
		if ( classesSet == null ) {
			classesSet = new HashSet<Class<?>>();
			classToIndexedClass.put( superclass, classesSet );
		}
		classesSet.add( indexedClass );
	}

	public Set<Class<?>> getIndexedClasses(Class<?>[] classes) {
		Set<Class<?>> indexedClasses = new HashSet<Class<?>>();
		for ( Class<?> clazz : classes ) {
			Set<Class<?>> set = classToIndexedClass.get( clazz );
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
