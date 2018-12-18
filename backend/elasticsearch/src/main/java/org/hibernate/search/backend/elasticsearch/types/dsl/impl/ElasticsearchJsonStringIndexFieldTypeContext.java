/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchJsonStringFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
public class ElasticsearchJsonStringIndexFieldTypeContext implements
		org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchJsonStringIndexFieldTypeContext,
		ElasticsearchIndexSchemaNodeContributor<PropertyMapping> {

	private static final Gson GSON = new GsonBuilder().create();

	// Must be a singleton so that equals() works as required by the interface
	private static final ElasticsearchJsonStringFieldCodec CODEC = new ElasticsearchJsonStringFieldCodec( GSON );

	private final IndexSchemaFieldDefinitionHelper<String> helper;

	private final String relativeFieldName;

	private final String mappingJsonString;

	public ElasticsearchJsonStringIndexFieldTypeContext(IndexSchemaContext schemaContext, String relativeFieldName, String mappingJsonString) {
		this.helper = new IndexSchemaFieldDefinitionHelper<>( schemaContext, String.class );
		this.relativeFieldName = relativeFieldName;
		this.mappingJsonString = mappingJsonString;
	}

	@Override
	public ElasticsearchJsonStringIndexFieldTypeContext dslConverter(
			ToDocumentFieldValueConverter<?, ? extends String> toIndexConverter) {
		helper.dslConverter( toIndexConverter );
		return this;
	}

	@Override
	public ElasticsearchJsonStringIndexFieldTypeContext projectionConverter(
			FromDocumentFieldValueConverter<? super String, ?> fromIndexConverter) {
		helper.projectionConverter( fromIndexConverter );
		return this;
	}

	@Override
	public IndexFieldAccessor<String> createAccessor() {
		return helper.createAccessor();
	}

	@Override
	public PropertyMapping contribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		PropertyMapping mapping = GSON.fromJson( mappingJsonString, PropertyMapping.class );

		ToDocumentFieldValueConverter<?, ? extends String> dslToIndexConverter =
				helper.createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super String, ?> indexToProjectionConverter =
				helper.createIndexToProjectionConverter();
		ElasticsearchJsonStringFieldCodec codec = CODEC;

		ElasticsearchIndexSchemaFieldNode<String> node = new ElasticsearchIndexSchemaFieldNode<>(
				parentNode, codec,
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( dslToIndexConverter, codec ),
				new ElasticsearchStandardFieldSortBuilderFactory<>( true, dslToIndexConverter, codec ),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( true, indexToProjectionConverter, codec )
		);

		JsonAccessor<JsonElement> jsonAccessor = JsonAccessor.root().property( relativeFieldName );
		helper.initialize( new ElasticsearchIndexFieldAccessor<>( jsonAccessor, node ) );

		String absoluteFieldPath = parentNode.getAbsolutePath( relativeFieldName );
		collector.collect( absoluteFieldPath, node );

		return mapping;
	}
}
