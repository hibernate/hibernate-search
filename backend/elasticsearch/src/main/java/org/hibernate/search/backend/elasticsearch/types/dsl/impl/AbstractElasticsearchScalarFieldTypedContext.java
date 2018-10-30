/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Projectable;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;

/**
 * @author Yoann Rodiere
 */
abstract class AbstractElasticsearchScalarFieldTypedContext<S extends AbstractElasticsearchScalarFieldTypedContext<? extends S, F>, F>
		extends AbstractElasticsearchStandardIndexSchemaFieldTypedContext<S, F> {

	private final DataType dataType;

	private Sortable sortable = Sortable.DEFAULT;
	protected boolean resolvedSortable;

	private Projectable projectable = Projectable.DEFAULT;
	protected boolean resolvedProjectable;

	AbstractElasticsearchScalarFieldTypedContext(IndexSchemaContext schemaContext,
			Class<F> fieldType, DataType dataType) {
		super( schemaContext, fieldType );
		this.dataType = dataType;
	}

	@Override
	public S projectable(Projectable projectable) {
		this.projectable = projectable;
		return thisAsS();
	}

	@Override
	public S sortable(Sortable sortable) {
		this.sortable = sortable;
		return thisAsS();
	}

	@Override
	protected PropertyMapping contribute(
			IndexSchemaFieldDefinitionHelper<F> helper,
			ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		PropertyMapping mapping = new PropertyMapping();

		mapping.setType( dataType );

		resolvedSortable = resolveDefault( sortable );
		resolvedProjectable = resolveDefault( projectable );

		mapping.setStore( resolvedProjectable );
		mapping.setDocValues( resolvedSortable );

		return mapping;
	}

}
