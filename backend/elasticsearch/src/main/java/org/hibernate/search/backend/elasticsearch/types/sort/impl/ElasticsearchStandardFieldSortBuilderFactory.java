/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchFieldSortBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.types.converter.impl.ElasticsearchFieldConverter;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class ElasticsearchStandardFieldSortBuilderFactory implements ElasticsearchFieldSortBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean sortable;

	private final ElasticsearchFieldConverter converter;

	public ElasticsearchStandardFieldSortBuilderFactory(boolean sortable, ElasticsearchFieldConverter converter) {
		this.sortable = sortable;
		this.converter = converter;
	}

	@Override
	public FieldSortBuilder<ElasticsearchSearchSortBuilder> createFieldSortBuilder(
			ElasticsearchSearchContext searchContext,
			String absoluteFieldPath) {
		checkSortable( absoluteFieldPath, sortable );

		return new ElasticsearchFieldSortBuilder( searchContext, absoluteFieldPath, converter );
	}

	@Override
	public DistanceSortBuilder<ElasticsearchSearchSortBuilder> createDistanceSortBuilder(String absoluteFieldPath,
			GeoPoint center) {
		throw log.distanceOperationsNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public boolean isDslCompatibleWith(ElasticsearchFieldSortBuilderFactory obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj.getClass() != ElasticsearchStandardFieldSortBuilderFactory.class ) {
			return false;
		}

		ElasticsearchStandardFieldSortBuilderFactory other = (ElasticsearchStandardFieldSortBuilderFactory) obj;

		return sortable == other.sortable &&
				converter.isConvertDslToIndexCompatibleWith( other.converter );
	}

	private static void checkSortable(String absoluteFieldPath, boolean sortable) {
		if ( !sortable ) {
			throw log.unsortableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}
}
