/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchJsonStringFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchJsonStringIndexFieldTypeContext;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.IndexFieldType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
class ElasticsearchJsonStringIndexFieldTypeContextImpl
		extends AbstractElasticsearchIndexFieldTypeConverterContext<ElasticsearchJsonStringIndexFieldTypeContextImpl, String>
		implements ElasticsearchJsonStringIndexFieldTypeContext<ElasticsearchJsonStringIndexFieldTypeContextImpl> {

	private static final Gson GSON = new GsonBuilder().create();

	// Must be a singleton so that equals() works as required by the interface
	private static final ElasticsearchJsonStringFieldCodec CODEC = new ElasticsearchJsonStringFieldCodec( GSON );

	private final String mappingJsonString;

	ElasticsearchJsonStringIndexFieldTypeContextImpl(ElasticsearchIndexFieldTypeBuildContext buildContext,
			String mappingJsonString) {
		super( buildContext, String.class );
		this.mappingJsonString = mappingJsonString;
	}

	@Override
	protected ElasticsearchJsonStringIndexFieldTypeContextImpl thisAsS() {
		return this;
	}

	@Override
	public IndexFieldType<String> toIndexFieldType() {
		PropertyMapping mapping = GSON.fromJson( mappingJsonString, PropertyMapping.class );

		ToDocumentFieldValueConverter<?, ? extends String> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super String, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		ElasticsearchJsonStringFieldCodec codec = CODEC;

		return new ElasticsearchIndexFieldType<>(
				codec,
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( dslToIndexConverter, codec ),
				new ElasticsearchStandardFieldSortBuilderFactory<>( true, dslToIndexConverter, codec ),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( true, indexToProjectionConverter, codec ),
				mapping
		);
	}
}
