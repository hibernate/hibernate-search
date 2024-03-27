/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Inspired from {@code org.hibernate.util.StringHelper}, but removing
 * most methods as they are not needed for Hibernate Search.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @deprecated Will be removed without replacement.
 */
@Deprecated
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

	/**
	 * Joins the elements of the given iterator to a string, separated by the given separator string.
	 *
	 * @param iterator the iterator to join
	 * @param separator the separator string
	 *
	 * @return a string made up of the string representations of the given iterator members, separated by the given separator
	 *         string
	 */
	public static String join(Iterator<?> iterator, String separator) {
		if ( iterator == null ) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;

		while ( iterator.hasNext() ) {
			if ( !isFirst ) {
				sb.append( separator );
			}
			else {
				isFirst = false;
			}

			sb.append( iterator.next() );
		}

		return sb.toString();
	}
}
