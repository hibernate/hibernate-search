/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

public class ElasticsearchTemporalFieldSortBuilderFactory<F extends TemporalAccessor>
		extends ElasticsearchStandardFieldSortBuilderFactory<F> {
	public ElasticsearchTemporalFieldSortBuilderFactory(boolean sortable,
			ElasticsearchFieldCodec<F> codec) {
		super( sortable, codec );
	}

	@Override
	public FieldSortBuilder createFieldSortBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<F> field) {
		checkSortable( field );

		return new ElasticsearchStandardFieldSort.TemporalFieldBuilder<>( searchContext, field, codec );
	}
}
