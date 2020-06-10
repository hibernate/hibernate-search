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
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchTextFieldSortBuilder extends ElasticsearchStandardFieldSortBuilder<String> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ElasticsearchTextFieldSortBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<String> field, ElasticsearchFieldCodec<String> codec) {
		super( searchContext, field, codec );
	}

	@Override
	public void mode(SortMode mode) {
		switch ( mode ) {
			case MIN:
			case MAX:
				super.mode( mode );
				break;
			case SUM:
			case AVG:
			case MEDIAN:
			default:
				throw log.cannotComputeSumOrAvgOrMedianForStringField( field.eventContext() );
		}
	}
}
