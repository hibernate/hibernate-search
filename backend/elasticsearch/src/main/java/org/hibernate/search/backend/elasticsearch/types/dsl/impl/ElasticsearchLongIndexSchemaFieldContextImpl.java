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
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.LongFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.converter.impl.StandardFieldConverter;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.StandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.StandardFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;

import com.google.gson.JsonElement;

public class ElasticsearchLongIndexSchemaFieldContextImpl
		extends AbstractElasticsearchScalarFieldTypedContext<ElasticsearchLongIndexSchemaFieldContextImpl, Long> {

	private final String relativeFieldName;

	public ElasticsearchLongIndexSchemaFieldContextImpl(IndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, Long.class, DataType.LONG );
		this.relativeFieldName = relativeFieldName;
	}

	@Override
	protected PropertyMapping contribute(IndexSchemaFieldDefinitionHelper<Long> helper,
			ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		PropertyMapping mapping = super.contribute( helper, collector, parentNode );

		StandardFieldConverter<Long> converter = new StandardFieldConverter<>(
				helper.createUserIndexFieldConverter(),
				LongFieldCodec.INSTANCE
		);

		ElasticsearchIndexSchemaFieldNode<Long> node = new ElasticsearchIndexSchemaFieldNode<>(
				parentNode, converter, LongFieldCodec.INSTANCE,
				new ElasticsearchStandardFieldPredicateBuilderFactory( converter ),
				new StandardFieldSortBuilderFactory( resolvedSortable, converter ),
				new StandardFieldProjectionBuilderFactory( resolvedProjectable, converter )
		);

		JsonAccessor<JsonElement> jsonAccessor = JsonAccessor.root().property( relativeFieldName );
		helper.initialize( new ElasticsearchIndexFieldAccessor<>( jsonAccessor, node ) );

		String absoluteFieldPath = parentNode.getAbsolutePath( relativeFieldName );
		collector.collect( absoluteFieldPath, node );

		return mapping;
	}

	@Override
	protected ElasticsearchLongIndexSchemaFieldContextImpl thisAsS() {
		return this;
	}
}
