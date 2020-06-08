/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types.values;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.spatial.GeoPoint;

public class IndexableGeoPointWithDistanceFromCenterValues extends IndexableValues<GeoPoint> {
	public static final GeoPoint CENTER_POINT_1 = GeoPoint.of( 46.038673, 3.978563 );
	public static final GeoPoint CENTER_POINT_2 = GeoPoint.of( 46.038683, 3.964652 );

	public static final IndexableGeoPointWithDistanceFromCenterValues INSTANCE = new IndexableGeoPointWithDistanceFromCenterValues();

	private final List<Double> singleDistancesFromCenterPoint1 = Collections.unmodifiableList( createDistancesFromCenterPoint1() );
	private final List<Double> singleDistancesFromCenterPoint2 = Collections.unmodifiableList( createDistancesFromCenterPoint2() );
	private final List<List<Double>> multiDistancesFromCenterPoint1 = makeMulti( singleDistancesFromCenterPoint1 );
	private final List<List<Double>> multiDistancesFromCenterPoint2 = makeMulti( singleDistancesFromCenterPoint2 );

	private IndexableGeoPointWithDistanceFromCenterValues() {
	}

	public List<Double> getSingleDistancesFromCenterPoint1() {
		return singleDistancesFromCenterPoint1;
	}

	public List<Double> getSingleDistancesFromCenterPoint2() {
		return singleDistancesFromCenterPoint2;
	}

	public List<List<Double>> getMultiDistancesFromCenterPoint1() {
		return multiDistancesFromCenterPoint1;
	}

	public List<List<Double>> getMultiDistancesFromCenterPoint2() {
		return multiDistancesFromCenterPoint2;
	}

	@Override
	protected List<GeoPoint> createSingle() {
		return asList(
				CENTER_POINT_1, // ~0km / ~1km
				CENTER_POINT_2, // ~1km / ~0km
				GeoPoint.of( 46.059852, 3.978235 ), // ~2km / ~2km
				GeoPoint.of( 46.039763, 3.914977 ), // ~5km / ~4km
				GeoPoint.of( 46.000833, 3.931265 ), // ~5.5km / ~5km
				GeoPoint.of( 46.094712, 4.044507 ), // ~8km / ~9km
				GeoPoint.of( 46.018378, 4.196792 ), // ~14km / ~13km
				GeoPoint.of( 46.123025, 3.845305 ) // ~17km / ~18km
		);
	}

	private List<Double> createDistancesFromCenterPoint1() {
		return asList(
				0.0,
				1_073.8,
				2_355.1,
				4_909.5,
				5_571.5,
				8_044.3,
				13_914.5,
				16_998.3
		);
	}

	private List<Double> createDistancesFromCenterPoint2() {
		return asList(
				1_073.8,
				0.0,
				2_576.7,
				3_836.2,
				4_935.5,
				8_761.8,
				13_141.2,
				18_063.5
		);
	}

}
