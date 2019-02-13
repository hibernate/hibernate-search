/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.backend.elasticsearch.util.impl.ElasticsearchFields;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.reporting.EventContext;

class ElasticsearchIndexSchemaFieldNodeBuilder<F>
		implements IndexSchemaFieldTerminalContext<IndexFieldAccessor<F>>,
		ElasticsearchIndexSchemaNodeContributor,
		IndexSchemaBuildContext {

	private final AbstractElasticsearchIndexSchemaObjectNodeBuilder parent;
	private final String relativeFieldName;
	private final String absoluteFieldPath;
	private final ElasticsearchIndexFieldType<F> type;

	private final IndexSchemaFieldDefinitionHelper<F> helper;

	ElasticsearchIndexSchemaFieldNodeBuilder(AbstractElasticsearchIndexSchemaObjectNodeBuilder parent,
			String relativeFieldName, ElasticsearchIndexFieldType<F> type) {
		this.parent = parent;
		this.relativeFieldName = relativeFieldName;
		this.absoluteFieldPath = ElasticsearchFields.compose( parent.getAbsolutePath(), relativeFieldName );
		this.type = type;
		this.helper = new IndexSchemaFieldDefinitionHelper<>( this );
	}

	@Override
	public EventContext getEventContext() {
		return parent.getRootNodeBuilder().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public IndexFieldAccessor<F> createAccessor() {
		return helper.createAccessor();
	}

	@Override
	public void contribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode,
			AbstractTypeMapping parentMapping) {
		IndexFieldAccessor<F> accessor = type.addField( collector, parentNode, parentMapping, relativeFieldName );
		helper.initialize( accessor );
	}

}
