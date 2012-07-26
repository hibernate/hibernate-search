package org.hibernate.search.spatial.impl;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldComparator;

public class DistanceComparator extends FieldComparator<Double> {

	private Point center;
	private Double bottomDistance;
	private int docBase = 0;
	private double[] distances;

	public DistanceComparator(Point center, int hitsCount) {
		this.center = center;
		this.distances = new double[ hitsCount ];
	}

	@Override
	public int compare(int slot1, int slot2) {
		return Double.compare( distances[ slot1 ], distances [ slot2 ]);
	}

	@Override
	public void setBottom(int slot) {
		bottomDistance = DistanceCache.DISTANCE_CACHE.get( center, docBase + slot );
	}

	@Override
	public int compareBottom(int doc) throws IOException {
		return Double.compare( bottomDistance, distances[ doc ]);
	}

	@Override
	public void copy(int slot, int doc) throws IOException {
		distances[ slot ] = DistanceCache.DISTANCE_CACHE.get(  center, docBase + doc );
		return;
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		this.docBase = docBase;
		return;
	}

	@Override
	public Double value(int slot) {
		return DistanceCache.DISTANCE_CACHE.get(  center, docBase + slot );
	}
}
