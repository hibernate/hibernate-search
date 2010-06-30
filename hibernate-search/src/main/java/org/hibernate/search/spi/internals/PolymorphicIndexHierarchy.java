package org.hibernate.search.spi.internals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import org.hibernate.search.util.LoggerFactory;

/**
 * Helper class which keeps track of all super classes and interfaces of the indexed entities.
 */
//FIXME make it immutable (builder pattern)
public class PolymorphicIndexHierarchy {
	private static final Logger log = LoggerFactory.make();

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
			log.trace( "Targeted indexed classes for {}: {}", Arrays.toString( classes ), indexedClasses );
		}
		return indexedClasses;
	}
}
