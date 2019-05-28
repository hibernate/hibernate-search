/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchJsonStringFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchNativeIndexFieldTypeContext;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.IndexFieldType;

import com.google.gson.Gson;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
class ElasticsearchNativeIndexFieldTypeContextImpl
		extends AbstractElasticsearchIndexFieldTypeConverterContext<ElasticsearchNativeIndexFieldTypeContextImpl, String>
		implements ElasticsearchNativeIndexFieldTypeContext<ElasticsearchNativeIndexFieldTypeContextImpl> {

	private final String mappingJsonString;

	ElasticsearchNativeIndexFieldTypeContextImpl(ElasticsearchIndexFieldTypeBuildContext buildContext,
			String mappingJsonString) {
		super( buildContext, String.class );
		this.mappingJsonString = mappingJsonString;
	}

	@Override
	protected ElasticsearchNativeIndexFieldTypeContextImpl thisAsS() {
		return this;
	}

	@Override
	public IndexFieldType<String> toIndexFieldType() {
		Gson gson = getBuildContext().getUserFacingGson();
		PropertyMapping mapping = gson.fromJson( mappingJsonString, PropertyMapping.class );

		ToDocumentFieldValueConverter<?, ? extends String> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super String, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		ElasticsearchJsonStringFieldCodec codec = new ElasticsearchJsonStringFieldCodec( gson );

		return new ElasticsearchIndexFieldType<>(
				codec,
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( true, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new ElasticsearchStandardFieldSortBuilderFactory<>( true, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( true, indexToProjectionConverter, createFromDocumentRawConverter(), codec ),
				mapping
		);
	}
}
