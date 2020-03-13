/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.hibernate.search.backend.lucene.search.sort.impl.AbstractLuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortCollector;
import org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl.LuceneGeoPointDistanceComparatorSource;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.search.SortField;

public class LuceneGeoPointDistanceSortBuilder extends AbstractLuceneSearchSortBuilder
		implements DistanceSortBuilder<LuceneSearchSortBuilder> {

	private final String absoluteFieldPath;
	private final String nestedDocumentPath;
	private final GeoPoint location;

	LuceneGeoPointDistanceSortBuilder(String absoluteFieldPath, String nestedDocumentPath, GeoPoint location) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.nestedDocumentPath = nestedDocumentPath;
		this.location = location;
	}

	@Override
	public void buildAndContribute(LuceneSearchSortCollector collector) {
		LuceneGeoPointDistanceComparatorSource fieldComparatorSource = new LuceneGeoPointDistanceComparatorSource(
				nestedDocumentPath, location );
		SortField sortField = new SortField( absoluteFieldPath, fieldComparatorSource, order == SortOrder.DESC );
		collector.collectSortField( sortField, ( nestedDocumentPath == null ) ? null : fieldComparatorSource );
	}
}
