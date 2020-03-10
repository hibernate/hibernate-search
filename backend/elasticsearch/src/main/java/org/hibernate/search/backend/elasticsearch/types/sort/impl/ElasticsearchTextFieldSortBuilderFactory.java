/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.sort.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchCompatibilityChecker;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;

public class ElasticsearchTextFieldSortBuilderFactory extends ElasticsearchStandardFieldSortBuilderFactory<String> {
	public ElasticsearchTextFieldSortBuilderFactory(boolean sortable,
			DslConverter<?, ? extends String> converter, DslConverter<String, ? extends String> rawConverter,
			ElasticsearchFieldCodec<String> codec) {
		super( sortable, converter, rawConverter, codec );
	}

	@Override
	public FieldSortBuilder<ElasticsearchSearchSortBuilder> createFieldSortBuilder(
			ElasticsearchSearchContext searchContext, String absoluteFieldPath, List<String> nestedPathHierarchy,
			ElasticsearchCompatibilityChecker converterChecker) {
		checkSortable( absoluteFieldPath );

		return new ElasticsearchTextFieldSortBuilder( searchContext, absoluteFieldPath, nestedPathHierarchy,
				converter, rawConverter, converterChecker, codec );
	}
}
