/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

class ElasticsearchIndexSchemaFieldNodeBuilder<F>
		implements IndexSchemaFieldOptionsStep<ElasticsearchIndexSchemaFieldNodeBuilder<F>, IndexFieldReference<F>>,
		ElasticsearchIndexSchemaNodeContributor,
		IndexSchemaBuildContext {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractElasticsearchIndexSchemaObjectNodeBuilder parent;
	private final String relativeFieldName;
	private final String absoluteFieldPath;
	private final IndexFieldInclusion inclusion;
	private final ElasticsearchIndexFieldType<F> type;
	private boolean multiValued = false;

	private ElasticsearchIndexFieldReference<F> reference;

	ElasticsearchIndexSchemaFieldNodeBuilder(AbstractElasticsearchIndexSchemaObjectNodeBuilder parent,
			String relativeFieldName, IndexFieldInclusion inclusion, ElasticsearchIndexFieldType<F> type) {
		this.parent = parent;
		this.relativeFieldName = relativeFieldName;
		this.absoluteFieldPath = FieldPaths.compose( parent.getAbsolutePath(), relativeFieldName );
		this.inclusion = inclusion;
		this.type = type;
	}

	@Override
	public EventContext getEventContext() {
		return parent.getRootNodeBuilder().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public ElasticsearchIndexSchemaFieldNodeBuilder<F> multiValued() {
		this.multiValued = true;
		return this;
	}

	@Override
	public IndexFieldReference<F> toReference() {
		if ( reference != null ) {
			throw log.cannotCreateReferenceMultipleTimes( getEventContext() );
		}
		this.reference = new ElasticsearchIndexFieldReference<>();
		return reference;
	}

	@Override
	public void contribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode,
			List<AbstractElasticsearchIndexSchemaFieldNode> staticChildrenForParent,
			AbstractTypeMapping parentMapping) {
		if ( reference == null ) {
			throw log.incompleteFieldDefinition( getEventContext() );
		}

		ElasticsearchIndexSchemaFieldNode<F> fieldNode = new ElasticsearchIndexSchemaFieldNode<>(
				parentNode, relativeFieldName, inclusion, multiValued, type
		);

		staticChildrenForParent.add( fieldNode );
		collector.collect( absoluteFieldPath, fieldNode );

		if ( IndexFieldInclusion.INCLUDED.equals( fieldNode.getInclusion() ) ) {
			parentMapping.addProperty( relativeFieldName, type.getMapping() );
		}

		reference.setSchemaNode( fieldNode );
	}

}
