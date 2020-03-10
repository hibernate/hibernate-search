/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import java.lang.invoke.MethodHandles;
import java.time.temporal.TemporalAccessor;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchTemporalFieldSortBuilder<F extends TemporalAccessor>
		extends ElasticsearchStandardFieldSortBuilder<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ElasticsearchTemporalFieldSortBuilder(ElasticsearchSearchContext searchContext,
			String absoluteFieldPath, List<String> nestedPathHierarchy,
			DslConverter<?, ? extends F> converter, DslConverter<F, ? extends F> rawConverter,
			ElasticsearchCompatibilityChecker converterChecker,
			ElasticsearchFieldCodec<F> codec) {
		super(
				searchContext, absoluteFieldPath, nestedPathHierarchy,
				converter, rawConverter, converterChecker,
				codec
		);
	}

	@Override
	public void mode(SortMode mode) {
		switch ( mode ) {
			case MIN:
			case MAX:
			case AVG:
			case MEDIAN:
				super.mode( mode );
				break;
			case SUM:
			default:
				throw log.cannotComputeSumForTemporalField( getEventContext() );
		}
	}
}
