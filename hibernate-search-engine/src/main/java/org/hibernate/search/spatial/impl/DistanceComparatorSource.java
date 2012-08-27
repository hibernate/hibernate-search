package org.hibernate.search.spatial.impl;

import java.io.IOException;

import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;

public class DistanceComparatorSource extends FieldComparatorSource {

	private Point center;

	public DistanceComparatorSource(Point center) {
		this.center = center;
	}

	@Override
	public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed)
			throws IOException {
		return new DistanceComparator( center, numHits, fieldname );
	}
}

