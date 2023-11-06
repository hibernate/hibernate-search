/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	@ValueSource(ints = { -1, -1000, 1025, 10000, Integer.MAX_VALUE, Integer.MIN_VALUE })
	void assertDimension(int dimension) {
		test( dimension, 5, 10, "dimension", dimension, 1024 );
	}

	@ParameterizedTest
	@ValueSource(ints = { -1, -1000, 3201, 10000, Integer.MAX_VALUE, Integer.MIN_VALUE })
	void assertBeamWidth(int beamWidth) {
		test( 2, beamWidth, 10, "beamWidth", beamWidth, 3200 );
	}

	@ParameterizedTest
	@ValueSource(ints = { -1, -1000, 513, 10000, Integer.MAX_VALUE, Integer.MIN_VALUE })
	void assertMaxConnections(int maxConnections) {
		test( 2, 2, maxConnections, "maxConnections", maxConnections, 512 );
	}

	void test(int dimension, int beamWidth, int maxConnections, String property, int value, int maxValue) {
		assertThatThrownBy( () -> setupHelper.start()
				.withIndex( SimpleMappedIndex
						.of( root -> root
								.field( "vector",
										f -> f.asByteVector().dimension( dimension ).beamWidth( beamWidth )
												.maxConnections( maxConnections ) )
								.toReference() ) )
				.setup()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Vector '" + property + "' cannot be equal to '" + value + "'",
						"It must be a positive integer value not greater than " + maxValue
				);
	}
}
