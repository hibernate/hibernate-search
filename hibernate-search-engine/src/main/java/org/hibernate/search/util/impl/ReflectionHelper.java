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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.util.StringHelper;

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
		catch ( SecurityException se ) {
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
		catch ( SecurityException se ) {
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
		catch ( Exception e ) {
			throw new IllegalStateException( "Could not get property value", e );
		}
		return value;
	}
}
