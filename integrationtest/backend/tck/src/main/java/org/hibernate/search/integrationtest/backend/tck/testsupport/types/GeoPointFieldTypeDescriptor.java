/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Optional;

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class GeoPointFieldTypeDescriptor extends FieldTypeDescriptor<GeoPoint> {

	GeoPointFieldTypeDescriptor() {
		super( GeoPoint.class );
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
	public Optional<FieldSortExpectations<GeoPoint>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<GeoPoint>(
				// The values are meaningless, we expect the sort to fail
				GeoPoint.of( 40, 70 ),
				GeoPoint.of( 40, 75 ),
				GeoPoint.of( 40, 80 ),
				GeoPoint.of( 0, 0 ),
				GeoPoint.of( 40, 72 ),
				GeoPoint.of( 40, 77 ),
				GeoPoint.of( 89, 89 )
		) {
			@Override
			public boolean isFieldSortSupported() {
				return false;
			}
		} );
	}

	@Override
	public Optional<FieldProjectionExpectations<GeoPoint>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				GeoPoint.of( 40, 70 ),
				GeoPoint.of( 40, 75 ),
				GeoPoint.of( 40, 80 )
		) );
	}
}
