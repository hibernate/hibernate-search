/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.util.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.util.StringHelper;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public abstract class ReflectionHelper {

	private ReflectionHelper() {
	}

	/**
	 * Get attribute name out of member unless overridden by <code>name</code>.
	 *
	 * @param member <code>XMember</code> from which to extract the name.
	 * @param name Override value which will be returned in case it is not empty.
	 *
	 * @return attribute name out of member unless overridden by <code>name</code>.
	 */
	public static String getAttributeName(XMember member, String name) {
		return StringHelper.isNotEmpty( name ) ?
				name :
				member.getName(); //explicit field name
	}

	/**
	 * Always use this method to set accessibility regardless of the visibility.
	 */
	public static void setAccessible(XMember member) {
		try {
			// always set accessible to true as it bypass the security model checks
			// at execution time and is faster.
			member.setAccessible( true );
		}
		catch (SecurityException se) {
			if ( !Modifier.isPublic( member.getModifiers() ) ) {
				throw se;
			}
		}
	}

	/**
	 * Always use this method to set accessibility regardless of the visibility.
	 */
	public static void setAccessible(AccessibleObject member) {
		try {
			// always set accessible to true as it bypass the security model checks
			// at execution time and is faster.
			member.setAccessible( true );
		}
		catch (SecurityException se) {
			if ( !Modifier.isPublic( ( (Member) member ).getModifiers() ) ) {
				throw se;
			}
		}
	}

	public static Object getMemberValue(Object bean, XMember getter) {
		Object value;
		try {
			value = getter.invoke( bean );
		}
		catch (Exception e) {
			throw new IllegalStateException( "Could not get property value", e );
		}
		return value;
	}

	/**
	 * Creates the class hierarchy for a given {@code XClass}.
	 *
	 * @param clazz the class for which to create the hierarchy
	 *
	 * @return the list of classes in the hierarchy starting at {@code java.lang.Object}
	 */
	public static List<XClass> createXClassHierarchy(XClass clazz) {
		List<XClass> hierarchy = new LinkedList<XClass>();
		XClass next;
		for ( XClass previousClass = clazz; previousClass != null; previousClass = next ) {
			next = previousClass.getSuperclass();
			if ( next != null ) {
				hierarchy.add( 0, previousClass ); // append to head to create a list in top-down iteration order
			}
		}
		return hierarchy;
	}

	/**
	 * Checks whether the specified class contains any Search specific annotations.
	 *
	 * @param mappedClass the {@code XClass} to check for Search annotations
	 *
	 * @return Returns {@code true} if the class contains at least one Search annotation, {@code false} otherwise
	 */
	public static boolean containsSearchAnnotations(XClass mappedClass) {
		// check the type annotations
		if ( containsSearchAnnotation( mappedClass.getAnnotations() ) ) {
			return true;
		}

		for ( XProperty method : mappedClass.getDeclaredProperties( XClass.ACCESS_PROPERTY ) ) {
			if ( containsSearchAnnotation( method.getAnnotations() ) ) {
				return true;
			}
		}

		for ( XProperty field : mappedClass.getDeclaredProperties( XClass.ACCESS_FIELD ) ) {
			if ( containsSearchAnnotation( field.getAnnotations() ) ) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if the annotation is a Search annotation by comparing the package of the annotation.
	 *
	 * @param annotation the annotation to check
	 *
	 * @return Returns {@code true} if the annotation is a Search annotation, {@code false} otherwise
	 */
	public static boolean isSearchAnnotation(Annotation annotation) {
		return "org.hibernate.search.annotations".equals( annotation.annotationType().getPackage().getName() );
	}

	private static boolean containsSearchAnnotation(Annotation[] annotations) {
		for ( Annotation annotation : annotations ) {
			if ( isSearchAnnotation( annotation ) ) {
				return true;
			}
		}
		return false;
	}
}
