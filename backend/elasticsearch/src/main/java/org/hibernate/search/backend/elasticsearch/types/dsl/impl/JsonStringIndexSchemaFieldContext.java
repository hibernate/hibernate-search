/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.converter.FromIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.ToIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.JsonStringFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.converter.impl.StandardFieldConverter;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.StandardFieldPredicateBuilderFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
public class JsonStringIndexSchemaFieldContext implements IndexSchemaFieldTypedContext<String>,
		ElasticsearchIndexSchemaNodeContributor<PropertyMapping> {

	private static final Gson GSON = new GsonBuilder().create();

	// Must be a singleton so that equals() works as required by the interface
	private static final JsonStringFieldCodec CODEC = new JsonStringFieldCodec( GSON );

	private final IndexSchemaFieldDefinitionHelper<String> helper;

	private final String relativeFieldName;

	private final String mappingJsonString;

	public JsonStringIndexSchemaFieldContext(IndexSchemaContext schemaContext, String relativeFieldName, String mappingJsonString) {
		this.helper = new IndexSchemaFieldDefinitionHelper<>( schemaContext, String.class );
		this.relativeFieldName = relativeFieldName;
		this.mappingJsonString = mappingJsonString;
	}

	@Override
	public IndexSchemaFieldTypedContext<String> dslConverter(
			ToIndexFieldValueConverter<?, ? extends String> toIndexConverter) {
		helper.dslConverter( toIndexConverter );
		return this;
	}

	@Override
	public IndexSchemaFieldTypedContext<String> projectionConverter(
			FromIndexFieldValueConverter<? super String, ?> fromIndexConverter) {
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

		StandardFieldConverter<String> converter = new StandardFieldConverter<>(
				helper.createUserIndexFieldConverter(),
				CODEC
		);
		StandardFieldPredicateBuilderFactory predicateBuilderFactory =
				new StandardFieldPredicateBuilderFactory( converter );
		ElasticsearchIndexSchemaFieldNode<String> node = new ElasticsearchIndexSchemaFieldNode<>(
				parentNode, converter, CODEC, predicateBuilderFactory
		);

		JsonAccessor<JsonElement> jsonAccessor = JsonAccessor.root().property( relativeFieldName );
		helper.initialize( new ElasticsearchIndexFieldAccessor<>( jsonAccessor, node ) );

		String absoluteFieldPath = parentNode.getAbsolutePath( relativeFieldName );
		collector.collect( absoluteFieldPath, node );

		return mapping;
	}
}
