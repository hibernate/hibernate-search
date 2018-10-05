/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.IntegerFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.converter.impl.StandardFieldConverter;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.StandardFieldPredicateBuilderFactory;

import com.google.gson.JsonElement;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
public class ElasticsearchIntegerIndexSchemaFieldContextImpl
		extends AbstractElasticsearchScalarFieldTypedContext<ElasticsearchIntegerIndexSchemaFieldContextImpl, Integer> {

	private final String relativeFieldName;

	public ElasticsearchIntegerIndexSchemaFieldContextImpl(IndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, Integer.class, DataType.INTEGER );
		this.relativeFieldName = relativeFieldName;
	}

	@Override
	protected PropertyMapping contribute(IndexSchemaFieldDefinitionHelper<Integer> helper,
			ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		PropertyMapping mapping = super.contribute( helper, collector, parentNode );

		StandardFieldConverter<Integer> converter = new StandardFieldConverter<>(
				helper.createUserIndexFieldConverter(),
				IntegerFieldCodec.INSTANCE
		);
		StandardFieldPredicateBuilderFactory predicateBuilderFactory =
				new StandardFieldPredicateBuilderFactory( converter );
		ElasticsearchIndexSchemaFieldNode<Integer> node = new ElasticsearchIndexSchemaFieldNode<>(
				parentNode, converter, IntegerFieldCodec.INSTANCE, predicateBuilderFactory
		);

		JsonAccessor<JsonElement> jsonAccessor = JsonAccessor.root().property( relativeFieldName );
		helper.initialize( new ElasticsearchIndexFieldAccessor<>( jsonAccessor, node ) );

		String absoluteFieldPath = parentNode.getAbsolutePath( relativeFieldName );
		collector.collect( absoluteFieldPath, node );

		return mapping;
	}

	@Override
	protected ElasticsearchIntegerIndexSchemaFieldContextImpl thisAsS() {
		return this;
	}
}
