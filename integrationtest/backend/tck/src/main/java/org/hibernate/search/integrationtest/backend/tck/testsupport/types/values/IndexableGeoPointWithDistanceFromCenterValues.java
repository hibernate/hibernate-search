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

	public static final IndexableGeoPointWithDistanceFromCenterValues INSTANCE =
			new IndexableGeoPointWithDistanceFromCenterValues();

	private final List<Double> singleDistancesFromCenterPoint1 =
			Collections.unmodifiableList( createDistancesFromCenterPoint1() );
	private final List<Double> singleDistancesFromCenterPoint2 =
			Collections.unmodifiableList( createDistancesFromCenterPoint2() );
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
				CENTER_POINT_1, // ~ 0km / ~ 1km
				CENTER_POINT_2, // ~ 1km / ~ 0km
				GeoPoint.of( 46.059852, 3.978235 ), // ~ 2km / ~ 3km
				GeoPoint.of( 46.039763, 3.914977 ), // ~ 5km / ~ 4km
				GeoPoint.of( 46.000833, 3.931265 ), // ~ 6km / ~ 5km
				GeoPoint.of( 46.094712, 4.044507 ), // ~ 8km / ~ 9km
				GeoPoint.of( 46.018378, 4.196792 ), // ~17km / ~18km
				GeoPoint.of( 46.123025, 3.845305 ), // ~14km / ~13km
				GeoPoint.of( 46.018378, 4.226792 ), // ~19km / ~20km
				GeoPoint.of( 46.018378, 4.286792 ), // ~24km / ~25km
				GeoPoint.of( 46.018378, 4.326792 ), // ~27km / ~28km
				GeoPoint.of( 46.018378, 4.386792 ), // ~32km / ~33km
				GeoPoint.of( 46.018378, 4.446792 ), // ~36km / ~37km
				GeoPoint.of( 46.018378, 4.506792 ), // ~41km / ~42km
				GeoPoint.of( 46.018378, 4.566792 ), // ~45km / ~47km
				GeoPoint.of( 46.018378, 4.626792 ), // ~50km / ~51km
				GeoPoint.of( 46.018378, 4.686792 ), // ~55km / ~56km
				GeoPoint.of( 46.018378, 4.746792 ), // ~59km / ~60km
				GeoPoint.of( 46.018378, 4.806792 ), // ~64km / ~65km
				GeoPoint.of( 46.018378, 4.866792 ), // ~69km / ~70km
				GeoPoint.of( 46.018378, 4.926792 ), // ~73km / ~74km
				GeoPoint.of( 46.018378, 4.986792 ), // ~78km / ~79km
				GeoPoint.of( 46.018378, 5.046792 ) // ~83km / ~84km
		);
	}

	private List<Double> createDistancesFromCenterPoint1() {
		return asList(
				0.0,
				1_073.8,
				2_355.1,
				4_909.6,
				5_571.5,
				8_044.4,
				16_998.3,
				13_914.6,
				19_296.4,
				23_902.9,
				26_978.8,
				31_597.1,
				36_218.9,
				40_843.1,
				45_468.8,
				50_095.8,
				54_723.6,
				59_352.1,
				63_981.1,
				68_610.5,
				73_240.2,
				77_870.2,
				82_500.5
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
				18_063.5,
				13_141.2,
				20_363.5,
				24_972.4,
				28_049.2,
				32_668.4,
				37_290.9,
				41_915.5,
				46_541.6,
				51_168.7,
				55_796.7,
				60_425.3,
				65_054.4,
				69_683.9,
				74_313.7,
				78_943.8,
				83_574.0
		);
	}

}
