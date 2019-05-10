/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.IndexFieldType;

public class ElasticsearchIndexFieldType<F> implements IndexFieldType<F> {
	private final ElasticsearchFieldCodec<F> codec;
	private final ElasticsearchFieldPredicateBuilderFactory predicateBuilderFactory;
	private final ElasticsearchFieldSortBuilderFactory sortBuilderFactory;
	private final ElasticsearchFieldProjectionBuilderFactory projectionBuilderFactory;
	private final PropertyMapping mapping;

	public ElasticsearchIndexFieldType(ElasticsearchFieldCodec<F> codec,
			ElasticsearchFieldPredicateBuilderFactory predicateBuilderFactory,
			ElasticsearchFieldSortBuilderFactory sortBuilderFactory,
			ElasticsearchFieldProjectionBuilderFactory projectionBuilderFactory,
			PropertyMapping mapping) {
		this.codec = codec;
		this.predicateBuilderFactory = predicateBuilderFactory;
		this.sortBuilderFactory = sortBuilderFactory;
		this.projectionBuilderFactory = projectionBuilderFactory;
		this.mapping = mapping;
	}

	public ElasticsearchIndexSchemaFieldNode<F> addField(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode, AbstractTypeMapping parentMapping,
			String relativeFieldName, boolean multiValued) {
		ElasticsearchIndexSchemaFieldNode<F> schemaNode = new ElasticsearchIndexSchemaFieldNode<>(
				parentNode,
				relativeFieldName,
				multiValued,
				codec,
				predicateBuilderFactory,
				sortBuilderFactory,
				projectionBuilderFactory
		);

		String absoluteFieldPath = parentNode.getAbsolutePath( relativeFieldName );
		collector.collect( absoluteFieldPath, schemaNode );

		parentMapping.addProperty( relativeFieldName, mapping );

		return schemaNode;
	}

	public void indexNullAs(F value) {
		mapping.setNullValue( codec.encode( value ) );
	}
}
