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
package org.hibernate.search.util;

import java.util.Arrays;

/**
 * Inspired from {@code org.hibernate.util.StringHelper}, but removing
 * most methods as they are not needed for Hibernate Search.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public final class StringHelper {

	private StringHelper() { /* static methods only - hide constructor */
	}

	public static boolean isNotEmpty(final String string) {
		return string != null && string.length() > 0;
	}

	public static boolean isEmpty(final String string) {
		return string == null || string.length() == 0;
	}

	public static String qualify(final String prefix, final String name) {
		if ( name == null || prefix == null ) {
			throw new NullPointerException();
		}

		return new StringBuilder( prefix.length() + name.length() + 1 )
				.append( prefix )
				.append( '.' )
				.append( name )
				.toString();
	}

	/**
	 * Joins the elements of the given array to a string, separated by the given separator string.
	 *
	 * @param array the array to join
	 * @param separator the separator string
	 *
	 * @return a string made up of the string representations of the given array's members, separated by the given separator
	 *         string
	 */
	public static String join(Object[] array, String separator) {
		return array != null ? join( Arrays.asList( array ), separator ) : null;
	}

	/**
	 * Joins the elements of the given iterable to a string, separated by the given separator string.
	 *
	 * @param iterable the iterable to join
	 * @param separator the separator string
	 *
	 * @return a string made up of the string representations of the given iterable members, separated by the given separator
	 *         string
	 */
	public static String join(Iterable<?> iterable, String separator) {
		if ( iterable == null ) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;

		for ( Object object : iterable ) {
			if ( !isFirst ) {
				sb.append( separator );
			}
			else {
				isFirst = false;
			}

			sb.append( object );
		}

		return sb.toString();
	}
}
