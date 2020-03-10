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
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchStandardFieldSortBuilderFactory<F> implements ElasticsearchFieldSortBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean sortable;

	protected final DslConverter<?, ? extends F> converter;
	protected final DslConverter<F, ? extends F> rawConverter;

	protected final ElasticsearchFieldCodec<F> codec;

	public ElasticsearchStandardFieldSortBuilderFactory(boolean sortable,
			DslConverter<?, ? extends F> converter, DslConverter<F, ? extends F> rawConverter,
			ElasticsearchFieldCodec<F> codec) {
		this.sortable = sortable;
		this.converter = converter;
		this.rawConverter = rawConverter;
		this.codec = codec;
	}

	@Override
	public FieldSortBuilder<ElasticsearchSearchSortBuilder> createFieldSortBuilder(
			ElasticsearchSearchContext searchContext,
			String absoluteFieldPath, List<String> nestedPathHierarchy, ElasticsearchCompatibilityChecker converterChecker) {
		checkSortable( absoluteFieldPath );

		return new ElasticsearchStandardFieldSortBuilder<>( searchContext, absoluteFieldPath, nestedPathHierarchy, converter, rawConverter, converterChecker, codec );
	}

	@Override
	public DistanceSortBuilder<ElasticsearchSearchSortBuilder> createDistanceSortBuilder(ElasticsearchSearchContext searchContext,
			String absoluteFieldPath, List<String> nestedPathHierarchy, GeoPoint center) {
		throw log.distanceOperationsNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public boolean hasCompatibleCodec(ElasticsearchFieldSortBuilderFactory obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj.getClass() != getClass() ) {
			return false;
		}

		ElasticsearchStandardFieldSortBuilderFactory<?> other = (ElasticsearchStandardFieldSortBuilderFactory<?>) obj;
		return sortable == other.sortable && codec.isCompatibleWith( other.codec );
	}

	@Override
	public boolean hasCompatibleConverter(ElasticsearchFieldSortBuilderFactory obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj.getClass() != getClass() ) {
			return false;
		}

		ElasticsearchStandardFieldSortBuilderFactory<?> other = (ElasticsearchStandardFieldSortBuilderFactory<?>) obj;
		return converter.isCompatibleWith( other.converter );
	}

	protected final void checkSortable(String absoluteFieldPath) {
		if ( !sortable ) {
			throw log.unsortableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}
}
