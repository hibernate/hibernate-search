/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.jar;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

public final class JandexTestUtils {
	private static final DotName RETENTION = DotName.createSimple( Retention.class.getName() );

	private JandexTestUtils() {
	}

	public static Set<String> toStrings(Collection<DotName> dotNames) {
		return dotNames.stream().map( DotName::toString ).collect( Collectors.toCollection( TreeSet::new ) );
	}

	public static Set<DotName> findRuntimeAnnotations(Index index) {
		Set<DotName> annotations = new HashSet<>();
		for ( AnnotationInstance retentionAnnotation : index.getAnnotations( RETENTION ) ) {
			ClassInfo annotation = retentionAnnotation.target().asClass();
			if ( RetentionPolicy.RUNTIME.name().equals( retentionAnnotation.value().asEnum() ) ) {
				annotations.add( annotation.name() );
			}
		}
		return annotations;
	}

	public static Set<DotName> collectClassHierarchiesRecursively(Index index, Set<DotName> initialClasses) {
		Set<DotName> classes = new HashSet<>();
		for ( DotName initialClass : initialClasses ) {
			collectClassHierarchiesRecursively( index, initialClass, classes );
		}
		return classes;
	}

	private static void collectClassHierarchiesRecursively(Index index, DotName className, Set<DotName> classes) {
		if ( className.toString().startsWith( "java." ) ) {
			return;
		}
		if ( classes.contains( className ) ) {
			return;
		}
		ClassInfo clazz = index.getClassByName( className );
		if ( clazz == null ) {
			return;
		}
		classes.add( className );
		collectClassHierarchiesRecursively( index, clazz.superName(), classes );
		for ( DotName interfaceName : clazz.interfaceNames() ) {
			collectClassHierarchiesRecursively( index, interfaceName, classes );
		}
		for ( ClassInfo subclass : index.getAllKnownSubclasses( className ) ) {
			collectClassHierarchiesRecursively( index, subclass.name(), classes );
		}
	}
}
