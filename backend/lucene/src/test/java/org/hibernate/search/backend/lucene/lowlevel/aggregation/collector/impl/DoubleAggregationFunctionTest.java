/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DoubleAggregationFunctionTest {

	public static Stream<Arguments> params() {
		return Stream.of(
				Arguments.of( new CompensatedSum(), apply( new CompensatedSum(), 1.0 ), 1.0 ),
				Arguments.of( apply( new CompensatedSum(), 1.0 ), new CompensatedSum(), 1.0 ),
				Arguments.of( apply( new CompensatedSum(), 1.0 ), apply( new CompensatedSum(), 1.0 ), 2.0 ),
				Arguments.of( apply( new CompensatedSum(), 10.0 ), apply( new CompensatedSum(), 20.0 ), 30.0 ),
				Arguments.of( new CompensatedSum(), new CompensatedSum(), null )
		);
	}

	private static DoubleAggregationFunction<?> apply(DoubleAggregationFunction<?> function, Double value) {
		function.apply( value );
		return function;
	}

	@ParameterizedTest
	@MethodSource("params")
	<T extends DoubleAggregationFunction<?>> void merge(DoubleAggregationFunction<T> agg1, DoubleAggregationFunction<T> agg2,
			Double expected) {
		agg1.merge( agg2 );

		assertThat( agg1.result() ).isEqualTo( expected );
	}
}
