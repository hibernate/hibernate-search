/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.common;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

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

	public static <T> T parseDiscreteValues(T[] allowedValues, Function<T, String> stringRepresentationFunction,
			BiFunction<String, List<String>, RuntimeException> invalidValueFunction,
			String value) {
		final String normalizedValue = value.trim().toLowerCase( Locale.ROOT );

		for ( T candidate : allowedValues ) {
			if ( stringRepresentationFunction.apply( candidate ).equals( normalizedValue ) ) {
				return candidate;
			}
		}

		throw invalidValueFunction.apply(
				normalizedValue,
				Arrays.stream( allowedValues )
						.map( stringRepresentationFunction )
						.collect( Collectors.toList() )
		);
	}
}
