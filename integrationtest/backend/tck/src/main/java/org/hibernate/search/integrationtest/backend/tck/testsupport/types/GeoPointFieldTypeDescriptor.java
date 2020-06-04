/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ExpectationsAlternative;

public class GeoPointFieldTypeDescriptor extends FieldTypeDescriptor<GeoPoint> {

	public static final GeoPointFieldTypeDescriptor INSTANCE = new GeoPointFieldTypeDescriptor();

	private GeoPointFieldTypeDescriptor() {
		super( GeoPoint.class );
	}

	@Override
	protected AscendingUniqueTermValues<GeoPoint> createAscendingUniqueTermValues() {
		return null; // Value lookup is not supported
	}

	@Override
	protected IndexableValues<GeoPoint> createIndexableValues() {
		return new IndexableValues<GeoPoint>() {
			@Override
			protected List<GeoPoint> createSingle() {
				return Arrays.asList(
						GeoPoint.of( 0.0, 0.0 ),
						// Negative 0 is a thing with doubles.
						GeoPoint.of( 0.0, -0.0 ),
						GeoPoint.of( -0.0, 0.0 ),
						GeoPoint.of( -0.0, -0.0 ),
						GeoPoint.of( 90.0, 0.0 ),
						GeoPoint.of( 90.0, 180.0 ),
						GeoPoint.of( 90.0, -180.0 ),
						GeoPoint.of( -90.0, 0.0 ),
						GeoPoint.of( -90.0, 180.0 ),
						GeoPoint.of( -90.0, -180.0 ),
						GeoPoint.of( 42.0, -42.0 ),
						GeoPoint.of( 40, 70 ),
						GeoPoint.of( 40, 75 ),
						GeoPoint.of( 40, 80 )
				);
			}
		};
	}

	@Override
	public Optional<MatchPredicateExpectations<GeoPoint>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<GeoPoint>(
				// The values are meaningless, we expect the match predicate to fail
				GeoPoint.of( 40, 70 ),
				GeoPoint.of( 45, 98 )
		) {
			@Override
			public boolean isMatchPredicateSupported() {
				return false;
			}
		} );
	}

	@Override
	public Optional<RangePredicateExpectations<GeoPoint>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<GeoPoint>(
				// The values are meaningless, we expect the range predicate to fail
				GeoPoint.of( 40, 70 ),
				GeoPoint.of( 40, 71 ),
				GeoPoint.of( 40, 72 ),
				GeoPoint.of( 30, 60 ),
				GeoPoint.of( 50, 80 )
		) {
			@Override
			public boolean isRangePredicateSupported() {
				return false;
			}
		} );
	}

	@Override
	public ExistsPredicateExpectations<GeoPoint> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				GeoPoint.of( 0.0, 0.0 ), GeoPoint.of( 40, 70 )
		);
	}

	@Override
	public ExpectationsAlternative<?, ?> getFieldSortExpectations() {
		return ExpectationsAlternative.unsupported( this );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<GeoPoint>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.empty();
	}
}
