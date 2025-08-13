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

public class AggregationFunctionTest {

	public static Stream<Arguments> params() {
		return Stream.of(
				Arguments.of( new Min(), apply( new Min(), 1L ), 1L ),
				Arguments.of( apply( new Min(), 1L ), new Min(), 1L ),
				Arguments.of( apply( new Min(), 1L ), apply( new Min(), 1L ), 1L ),
				Arguments.of( apply( new Min(), 10L ), apply( new Min(), 20L ), 10L ),
				Arguments.of( new Min(), new Min(), null ),

				Arguments.of( new Max(), apply( new Max(), 1L ), 1L ),
				Arguments.of( apply( new Max(), 1L ), new Max(), 1L ),
				Arguments.of( apply( new Max(), 1L ), apply( new Max(), 1L ), 1L ),
				Arguments.of( apply( new Max(), 10L ), apply( new Max(), 20L ), 20L ),
				Arguments.of( new Max(), new Max(), null ),

				Arguments.of( new Sum(), apply( new Sum(), 1L ), 1L ),
				Arguments.of( apply( new Sum(), 1L ), new Sum(), 1L ),
				Arguments.of( apply( new Sum(), 1L ), apply( new Sum(), 1L ), 2L ),
				Arguments.of( apply( new Sum(), 10L ), apply( new Sum(), 20L ), 30L ),
				Arguments.of( new Sum(), new Sum(), null ),

				Arguments.of( new CountValues(), apply( new CountValues(), 1L ), 1L ),
				Arguments.of( apply( new CountValues(), 1L ), new CountValues(), 1L ),
				Arguments.of( apply( new CountValues(), 1L ), apply( new CountValues(), 1L ), 2L ),
				Arguments.of( apply( new CountValues(), 10L ), apply( new CountValues(), 20L ), 2L ),
				Arguments.of( new CountValues(), new CountValues(), 0L ),

				Arguments.of( new CountDistinctValues(), apply( new CountDistinctValues(), 1L ), 1L ),
				Arguments.of( apply( new CountDistinctValues(), 1L ), new CountDistinctValues(), 1L ),
				Arguments.of( apply( new CountDistinctValues(), 1L ), apply( new CountDistinctValues(), 1L ), 1L ),
				Arguments.of( apply( new CountDistinctValues(), 10L ), apply( new CountDistinctValues(), 20L ), 2L ),
				Arguments.of( new CountDistinctValues(), new CountDistinctValues(), 0L )
		);
	}

	private static AggregationFunction<?> apply(AggregationFunction<?> function, Long value) {
		function.apply( value );
		return function;
	}

	@ParameterizedTest
	@MethodSource("params")
	<T extends AggregationFunction<?>> void merge(AggregationFunction<T> agg1, AggregationFunction<T> agg2, Long expected) {
		agg1.merge( agg2 );

		assertThat( agg1.result() ).isEqualTo( expected );
	}
}
