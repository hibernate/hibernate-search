package org.hibernate.search.spatial.impl;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.Scorer;

public class DistanceCollector extends Collector {

	private Collector delegate;
	private boolean acceptsDocsOutOfOrder;

	private Point center;
	private String latitudeField;
	private String longitudeField;
	private int docBase = 0;
	private double[] distances;
	private double[] latitudeValues;
	private double[] longitudeValues;

	public DistanceCollector(Collector delegate, Point center, int hitsCount, String fieldname) {
		this.delegate= delegate;
		this.acceptsDocsOutOfOrder = delegate.acceptsDocsOutOfOrder();
		this.center = center;
		this.distances = new double[ hitsCount ];
		this.latitudeValues = new double[ hitsCount ];
		this.longitudeValues = new double[ hitsCount ];
		this.latitudeField = GridHelper.formatLatitude( fieldname );
		this.longitudeField = GridHelper.formatLongitude(  fieldname );
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		delegate.setScorer( scorer );
		return;
	}

	@Override
	public void collect(int doc) throws IOException {
		delegate.collect( doc );
		distances[ docBase + doc] = center.getDistanceTo( latitudeValues[ docBase + doc ], longitudeValues[ docBase + doc ] );
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		delegate.setNextReader( reader, docBase );
		double[] unbasedLatitudeValues = FieldCache.DEFAULT.getDoubles( reader, latitudeField );
		double[] unbasedLongitudeValues = FieldCache.DEFAULT.getDoubles( reader, longitudeField );
		this.docBase = docBase;
		for ( int i = 0 ; i < unbasedLatitudeValues.length ; i ++ ) {
			latitudeValues[ this.docBase + i ] = unbasedLatitudeValues[ i ];
			longitudeValues[ this.docBase + i ] = unbasedLongitudeValues[ i ];
		}
		return;
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return acceptsDocsOutOfOrder;
	}

	public double getDistance(int index) {
		return distances[ index ];
	}
}
