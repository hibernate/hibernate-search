/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.mapping;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LuceneVectorFieldIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@ParameterizedTest
	@ValueSource(ints = { -1, -1000, 4097, 10000, Integer.MAX_VALUE, Integer.MIN_VALUE })
	void assertDimension(int dimension) {
		test( dimension, 5, 10, "dimension", dimension, 4096 );
	}

	@ParameterizedTest
	@ValueSource(ints = { -1, -1000, 3201, 10000, Integer.MAX_VALUE, Integer.MIN_VALUE })
	void assertEfConstruction(int efConstruction) {
		test( 2, efConstruction, 10, "efConstruction", efConstruction, 3200 );
	}

	@ParameterizedTest
	@ValueSource(ints = { -1, -1000, 513, 10000, Integer.MAX_VALUE, Integer.MIN_VALUE })
	void assertM(int m) {
		test( 2, 2, m, "m", m, 512 );
	}

	void test(int dimension, int efConstruction, int m, String property, int value, int maxValue) {
		assertThatThrownBy( () -> setupHelper.start()
				.withIndex( SimpleMappedIndex
						.of( root -> root
								.field( "vector",
										f -> f.asByteVector().dimension( dimension ).efConstruction( efConstruction )
												.m( m ) )
								.toReference() ) )
				.setup()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Vector '" + property + "' cannot be equal to '" + value + "'",
						"It must be a positive integer value lesser than or equal to " + maxValue
				);
	}
}
