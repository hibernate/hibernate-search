/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.impl;

import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.hibernate.search.util.common.AssertionFailure;

public final class StreamHelper {

	private StreamHelper() {
	}

	@SuppressWarnings("unchecked")
	public static <T, E extends RuntimeException> Collector<T, ?, T> singleElement(
			Supplier<E> missingValueExceptionSupplier, Supplier<E> multipleValuesExceptionSupplier) {
		return Collector.<T, Optional<T>[], T>of(
				() -> new Optional[1],
				(holder, value) -> {
					if ( holder[0] != null ) {
						throw multipleValuesExceptionSupplier.get();
					}
					// Preserve null values by wrapping values in an Optional
					holder[0] = Optional.ofNullable( value );
				},
				(holder1, holder2) -> {
					if ( holder1[0] == null ) {
						return holder2;
					}
					else if ( holder2[0] != null ) {
						throw multipleValuesExceptionSupplier.get();
					}
					else {
						return holder1;
					}
				},
				holder -> {
					if ( holder[0] == null ) {
						throw missingValueExceptionSupplier.get();
					}
					// Restore null values
					return holder[0].orElse( null );
				} );
	}

	public static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(
			Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper,
			Supplier<M> mapSupplier) {
		return Collectors.toMap( keyMapper, valueMapper, throwingMerger(), mapSupplier );
	}

	private static <T> BinaryOperator<T> throwingMerger() {
		return (u, v) -> { throw new AssertionFailure( "Unexpected duplicate key: " + u ); };
	}

}
