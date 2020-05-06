/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.impl;

import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneGeoPointFieldCodec;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

public class LuceneGeoPointFieldSortBuilderFactory
		extends AbstractLuceneFieldSortBuilderFactory<GeoPoint, LuceneGeoPointFieldCodec> {

	public LuceneGeoPointFieldSortBuilderFactory(boolean sortable, LuceneGeoPointFieldCodec codec) {
		super( sortable, codec );
	}

	@Override
	public FieldSortBuilder<LuceneSearchSortBuilder> createFieldSortBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath, String nestedDocumentPath, LuceneCompatibilityChecker converterChecker) {
		throw log.traditionalSortNotSupportedByGeoPoint(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public DistanceSortBuilder<LuceneSearchSortBuilder> createDistanceSortBuilder(String absoluteFieldPath,
			String nestedDocumentPath, GeoPoint center) {
		checkSortable( absoluteFieldPath );

		return new LuceneGeoPointDistanceSortBuilder( absoluteFieldPath, nestedDocumentPath, center );
	}

	@Override
	public boolean hasCompatibleConverter(LuceneFieldSortBuilderFactory other) {
		return true;
	}

}
