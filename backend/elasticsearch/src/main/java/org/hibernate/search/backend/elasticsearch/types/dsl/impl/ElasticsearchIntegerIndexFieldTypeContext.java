/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchStandardFieldProjectionBuilderFactory;
import org.hibernate.search.engine.backend.document.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchIntegerFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;

import com.google.gson.JsonElement;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
public class ElasticsearchIntegerIndexFieldTypeContext
		extends AbstractElasticsearchScalarFieldTypeContext<ElasticsearchIntegerIndexFieldTypeContext, Integer> {

	private final String relativeFieldName;

	public ElasticsearchIntegerIndexFieldTypeContext(IndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, Integer.class, DataType.INTEGER );
		this.relativeFieldName = relativeFieldName;
	}

	@Override
	protected PropertyMapping contribute(IndexSchemaFieldDefinitionHelper<Integer> helper,
			ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		PropertyMapping mapping = super.contribute( helper, collector, parentNode );

		ToDocumentFieldValueConverter<?, ? extends Integer> dslToIndexConverter =
				helper.createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super Integer, ?> indexToProjectionConverter =
				helper.createIndexToProjectionConverter();
		ElasticsearchIntegerFieldCodec codec = ElasticsearchIntegerFieldCodec.INSTANCE;

		ElasticsearchIndexSchemaFieldNode<Integer> node = new ElasticsearchIndexSchemaFieldNode<>(
				parentNode, codec,
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( dslToIndexConverter, codec ),
				new ElasticsearchStandardFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, codec ),
				new ElasticsearchStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, codec )
		);

		JsonAccessor<JsonElement> jsonAccessor = JsonAccessor.root().property( relativeFieldName );
		helper.initialize( new ElasticsearchIndexFieldAccessor<>( jsonAccessor, node ) );

		String absoluteFieldPath = parentNode.getAbsolutePath( relativeFieldName );
		collector.collect( absoluteFieldPath, node );

		return mapping;
	}

	@Override
	protected ElasticsearchIntegerIndexFieldTypeContext thisAsS() {
		return this;
	}
}
