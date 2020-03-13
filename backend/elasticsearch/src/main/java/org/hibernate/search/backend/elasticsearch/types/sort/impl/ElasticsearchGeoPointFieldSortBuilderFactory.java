/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchGeoPointFieldSortBuilderFactory implements ElasticsearchFieldSortBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean sortable;

	public ElasticsearchGeoPointFieldSortBuilderFactory(boolean sortable) {
		this.sortable = sortable;
	}

	@Override
	public FieldSortBuilder<ElasticsearchSearchSortBuilder> createFieldSortBuilder(
			ElasticsearchSearchContext searchContext,
			String absoluteFieldPath, List<String> nestedPathHierarchy, ElasticsearchCompatibilityChecker converterChecker) {
		throw log.traditionalSortNotSupportedByGeoPoint(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public DistanceSortBuilder<ElasticsearchSearchSortBuilder> createDistanceSortBuilder(ElasticsearchSearchContext searchContext,
			String absoluteFieldPath, List<String> nestedPathHierarchy, GeoPoint center) {
		checkSortable( absoluteFieldPath, sortable );

		return new ElasticsearchDistanceSortBuilder( searchContext, absoluteFieldPath, nestedPathHierarchy, center );
	}

	@Override
	public boolean hasCompatibleCodec(ElasticsearchFieldSortBuilderFactory other) {
		if ( other.getClass() != this.getClass() ) {
			return false;
		}

		ElasticsearchGeoPointFieldSortBuilderFactory otherFactory = (ElasticsearchGeoPointFieldSortBuilderFactory) other;
		return otherFactory.sortable == this.sortable;
	}

	@Override
	public boolean hasCompatibleConverter(ElasticsearchFieldSortBuilderFactory obj) {
		return true;
	}

	private static void checkSortable(String absoluteFieldPath, boolean sortable) {
		if ( !sortable ) {
			throw log.unsortableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}
}
