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
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class GeoPointFieldTypeDescriptor extends StandardFieldTypeDescriptor<GeoPoint> {

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
	protected List<GeoPoint> createUniquelyMatchableValues() {
		return Arrays.asList(
				GeoPoint.of( 0.0, 0.0 ),
				GeoPoint.of( 42.0, -42.0 ),
				GeoPoint.of( 40, 70 ),
				GeoPoint.of( 40, 75 ),
				GeoPoint.of( 40, 80 )
		);
	}

	@Override
	protected List<GeoPoint> createNonMatchingValues() {
		return Arrays.asList(
				GeoPoint.of( 739, 739 ),
				GeoPoint.of( 739, -739 ),
				GeoPoint.of( -739, 739 ),
				GeoPoint.of( -739, -739 )
		);
	}

	@Override
	public GeoPoint valueFromInteger(int integer) {
		return GeoPoint.of( 0, integer );
	}

	@Override
	public boolean isFieldSortSupported() {
		return false;
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<GeoPoint>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.empty();
	}
}
