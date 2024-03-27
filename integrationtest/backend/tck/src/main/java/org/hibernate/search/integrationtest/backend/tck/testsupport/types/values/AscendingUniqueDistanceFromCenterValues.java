/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types.values;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.spatial.GeoPoint;

public class AscendingUniqueDistanceFromCenterValues extends AscendingUniqueTermValues<GeoPoint> {
	public static final GeoPoint CENTER_POINT = GeoPoint.of( 46.038673, 3.978563 );

	public static final AscendingUniqueDistanceFromCenterValues INSTANCE = new AscendingUniqueDistanceFromCenterValues();

	private final List<Double> singleDistancesFromCenterPoint;
	private final List<List<Double>> multiDistancesFromCenterPointForMinDataset;

	private AscendingUniqueDistanceFromCenterValues() {
		this.singleDistancesFromCenterPoint = Collections.unmodifiableList( createDistancesFromCenterPoint() );
		this.multiDistancesFromCenterPointForMinDataset = Collections.unmodifiableList(
				makeMultiDistances( getMultiResultingInSingle( SortMode.MIN ) )
		);
	}

	public List<Double> getSingleDistancesFromCenterPoint() {
		return singleDistancesFromCenterPoint;
	}

	public List<List<Double>> getMultiDistancesFromCenterPointForMinDataset() {
		return multiDistancesFromCenterPointForMinDataset;
	}

	@Override
	protected List<GeoPoint> createSingle() {
		return asList(
				CENTER_POINT, // ~0km
				GeoPoint.of( 46.038683, 3.964652 ), // ~1km
				GeoPoint.of( 46.059852, 3.978235 ), // ~2km
				GeoPoint.of( 46.039763, 3.914977 ), // ~5km
				GeoPoint.of( 46.000833, 3.931265 ), // ~55.5km
				GeoPoint.of( 46.094712, 4.044507 ), // ~8km
				GeoPoint.of( 46.123025, 3.845305 ), // ~14km
				GeoPoint.of( 46.018378, 4.196792 ) // ~17km
		);
	}

	@Override
	protected List<List<GeoPoint>> createMultiResultingInSingleAfterSum() {
		return valuesThatWontBeUsed();
	}

	@Override
	protected List<List<GeoPoint>> createMultiResultingInSingleAfterAvg() {
		return asList(
				asList( CENTER_POINT, CENTER_POINT ), // ~0km
				asList( getSingle().get( 0 ), getSingle().get( 2 ) ), // ~1km
				asList( getSingle().get( 1 ), getSingle().get( 1 ), getSingle().get( 4 ) ), // ~2km
				asList( getSingle().get( 2 ), getSingle().get( 4 ) ), // ~4km
				asList( getSingle().get( 3 ), getSingle().get( 5 ) ), // ~6km
				asList( getSingle().get( 4 ), getSingle().get( 6 ) ), // ~8km
				asList( getSingle().get( 4 ), getSingle().get( 7 ) ), // ~10km
				asList( getSingle().get( 7 ), getSingle().get( 7 ), getSingle().get( 7 ) ) // ~14km
		);
	}

	private List<Double> createDistancesFromCenterPoint() {
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

	private List<List<Double>> makeMultiDistances(List<List<GeoPoint>> multiPoints) {
		List<List<Double>> result = new ArrayList<>();
		for ( List<GeoPoint> multiPoint : multiPoints ) {
			List<Double> multiDistance = new ArrayList<>();
			result.add( multiDistance );
			for ( GeoPoint geoPoint : multiPoint ) {
				int index = getSingle().indexOf( geoPoint );
				multiDistance.add( singleDistancesFromCenterPoint.get( index ) );
			}
		}
		return result;
	}
}
