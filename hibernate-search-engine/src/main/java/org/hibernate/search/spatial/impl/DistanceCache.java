package org.hibernate.search.spatial.impl;

import java.util.HashMap;
import java.util.WeakHashMap;

public class DistanceCache {
	private WeakHashMap<Point,HashMap<Integer,Double>> cache;

	private DistanceCache( ) {
		cache = new WeakHashMap<Point, HashMap<Integer, Double>>( );
	}

	public static DistanceCache DISTANCE_CACHE =  new DistanceCache( );

	public void put( Point point, Integer docNum, Double distance ) {
		HashMap<Integer,Double> distanceMap= cache.get( point );
		if ( distanceMap == null ) {
			distanceMap= new HashMap<Integer, Double>( );
			cache.put( point,  distanceMap );
		}

		distanceMap.put( docNum, distance);
	}

	public Double get( Point point, Integer docNum ) {
		HashMap<Integer,Double> distanceMap= cache.get( point );
		if ( distanceMap == null ) {
			return null;
		}

		return distanceMap.get( docNum );
	}
}
