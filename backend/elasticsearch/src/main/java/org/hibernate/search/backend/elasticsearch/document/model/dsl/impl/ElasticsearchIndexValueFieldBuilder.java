/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexCompositeNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexValueField;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

class ElasticsearchIndexValueFieldBuilder<F>
		implements IndexSchemaFieldOptionsStep<ElasticsearchIndexValueFieldBuilder<F>, IndexFieldReference<F>>,
		ElasticsearchIndexNodeContributor, IndexSchemaBuildContext {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractElasticsearchIndexCompositeNodeBuilder parent;
	private final String relativeFieldName;
	private final String absoluteFieldPath;
	private final TreeNodeInclusion inclusion;
	private final ElasticsearchIndexValueFieldType<F> type;
	private boolean multiValued = false;

	private ElasticsearchIndexFieldReference<F> reference;

	ElasticsearchIndexValueFieldBuilder(AbstractElasticsearchIndexCompositeNodeBuilder parent,
			String relativeFieldName, TreeNodeInclusion inclusion, ElasticsearchIndexValueFieldType<F> type) {
		this.parent = parent;
		this.relativeFieldName = relativeFieldName;
		this.absoluteFieldPath = FieldPaths.compose( parent.getAbsolutePath(), relativeFieldName );
		this.inclusion = inclusion;
		this.type = type;
	}

	@Override
	public EventContext eventContext() {
		return parent.getRootNodeBuilder().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public ElasticsearchIndexValueFieldBuilder<F> multiValued() {
		if ( !type.multivaluable() ) {
			throw log.multiValuedFieldNotAllowed( eventContext() );
		}
		this.multiValued = true;
		return this;
	}

	@Override
	public IndexFieldReference<F> toReference() {
		if ( reference != null ) {
			throw log.cannotCreateReferenceMultipleTimes( eventContext() );
		}
		this.reference = new ElasticsearchIndexFieldReference<>();
		return reference;
	}

	@Override
	public void contribute(ElasticsearchIndexNodeCollector collector,
			ElasticsearchIndexCompositeNode parentNode,
			Map<String, ElasticsearchIndexField> staticChildrenByNameForParent,
			AbstractTypeMapping parentMapping) {
		if ( reference == null ) {
			throw log.incompleteFieldDefinition( eventContext() );
		}

		ElasticsearchIndexValueField<F> fieldNode = new ElasticsearchIndexValueField<>(
				parentNode, relativeFieldName, type, inclusion, multiValued );

		staticChildrenByNameForParent.put( relativeFieldName, fieldNode );
		collector.collect( absoluteFieldPath, fieldNode );

		if ( TreeNodeInclusion.INCLUDED.equals( fieldNode.inclusion() ) ) {
			parentMapping.addProperty( relativeFieldName, type.mapping() );
			type.additionalIndexSettings().ifPresent( c -> c.accept( collector.propertyMappingIndexSettingsContributor() ) );
		}

		reference.setSchemaNode( fieldNode );
	}

}
