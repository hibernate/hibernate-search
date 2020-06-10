/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

public class ElasticsearchTextFieldSortBuilderFactory extends ElasticsearchStandardFieldSortBuilderFactory<String> {
	public ElasticsearchTextFieldSortBuilderFactory(boolean sortable,
			ElasticsearchFieldCodec<String> codec) {
		super( sortable, codec );
	}

	@Override
	public FieldSortBuilder<ElasticsearchSearchSortBuilder> createFieldSortBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<String> field) {
		checkSortable( field );
		return new ElasticsearchTextFieldSortBuilder( searchContext, field, codec );
	}
}
