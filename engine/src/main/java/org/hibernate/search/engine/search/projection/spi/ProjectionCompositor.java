/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.search.spi.ResultsCompositor;
import org.hibernate.search.util.common.function.TriFunction;

/**
 * A variation on {@link java.util.stream.Collector} suitable for composing the result of inner projections
 * in a composite projection.
 * <p>
 * Compared to {@link java.util.stream.Collector}:
 * <ul>
 *     <li>There is no concept of parallel execution.</li>
 *     <li>All operations are expected to be non-blocking,
 *     except for {@link #finish(Object)}</li>
 *     <li>The number of component values is known in advance,
 *     and clients are expected to {@link #set(Object, int, Object) set} exactly that number of values every time.</li>
 *     <li>The type of component values is flexible and the values can be mutated before they are composed,
 *     but each component values is expected to have a specific type upon calling {@link #finish(Object)}.
 *     Clients are responsible for ensuring component values have the required type
 *     upon calling {@link #finish(Object)}.
 * </ul>
 *
 * @param <E> The type of the temporary storage for component values.
 * @param <V> The type of the final result representing a composed value.
 *
 * @deprecated Use {@link ResultsCompositor} instead.
 */
@SuppressWarnings({ "deprecation", "removal" })
@Deprecated(since = "8.1", forRemoval = true)
public interface ProjectionCompositor<E, V> extends ResultsCompositor<E, V> {

	static <P1, V> ProjectionCompositor<Object, V> from(Function<P1, V> transformer) {
		return new SingleValuedProjectionCompositor<>( transformer );
	}

	static <P1, P2, V> ProjectionCompositor<Object[], V> from(BiFunction<P1, P2, V> transformer) {
		return new ObjectArrayProjectionCompositor<V>( 2 ) {
			@SuppressWarnings("unchecked")
			@Override
			public V finish(Object[] components) {
				return transformer.apply( (P1) components[0], (P2) components[1] );
			}

			@Override
			protected Object transformer() {
				return transformer;
			}
		};
	}

	static <P1, P2, P3, V> ProjectionCompositor<Object[], V> from(TriFunction<P1, P2, P3, V> transformer) {
		return new ObjectArrayProjectionCompositor<V>( 3 ) {
			@SuppressWarnings("unchecked")
			@Override
			public V finish(Object[] components) {
				return transformer.apply( (P1) components[0], (P2) components[1], (P3) components[2] );
			}

			@Override
			protected Object transformer() {
				return transformer;
			}
		};
	}

	static ProjectionCompositor<Object[], List<?>> fromList(int size) {
		return fromArray( size, Arrays::asList );
	}

	static <V> ProjectionCompositor<Object[], V> fromList(int size, Function<? super List<?>, ? extends V> transformer) {
		return fromArray( size, transformer.compose( Arrays::asList ) );
	}

	static ProjectionCompositor<Object[], Object[]> fromArray(int size) {
		return fromArray( size, Function.identity() );
	}

	static <V> ProjectionCompositor<Object[], V> fromArray(int size, Function<? super Object[], ? extends V> transformer) {
		return new ObjectArrayProjectionCompositor<V>( size ) {
			@Override
			public V finish(Object[] components) {
				return transformer.apply( components );
			}

			@Override
			protected Object transformer() {
				return transformer;
			}
		};
	}

}
