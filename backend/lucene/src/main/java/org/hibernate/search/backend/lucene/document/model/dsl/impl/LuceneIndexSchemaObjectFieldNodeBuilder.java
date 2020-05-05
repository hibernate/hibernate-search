/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.lucene.document.model.impl.AbstractLuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectFieldNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexObjectFieldReference;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;

class LuceneIndexSchemaObjectFieldNodeBuilder extends AbstractLuceneIndexSchemaObjectNodeBuilder
		implements IndexSchemaObjectFieldNodeBuilder, LuceneIndexSchemaNodeContributor {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractLuceneIndexSchemaObjectNodeBuilder parent;
	private final String absoluteFieldPath;
	private final String relativeFieldName;
	private final IndexFieldInclusion inclusion;
	private final ObjectFieldStorage storage;
	private boolean multiValued = false;

	private LuceneIndexObjectFieldReference reference;

	LuceneIndexSchemaObjectFieldNodeBuilder(AbstractLuceneIndexSchemaObjectNodeBuilder parent,
			String relativeFieldName, IndexFieldInclusion inclusion, ObjectFieldStorage storage) {
		this.parent = parent;
		this.absoluteFieldPath = FieldPaths.compose( parent.getAbsolutePath(), relativeFieldName );
		this.relativeFieldName = relativeFieldName;
		this.inclusion = inclusion;
		this.storage = storage;
	}

	@Override
	public EventContext getEventContext() {
		return getRootNodeBuilder().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public void multiValued() {
		this.multiValued = true;
	}

	@Override
	public IndexObjectFieldReference toReference() {
		if ( reference != null ) {
			throw log.cannotCreateReferenceMultipleTimes( getEventContext() );
		}
		this.reference = new LuceneIndexObjectFieldReference();
		return reference;
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode,
			List<AbstractLuceneIndexSchemaFieldNode> staticChildrenForParent) {
		if ( reference == null ) {
			throw log.incompleteFieldDefinition( getEventContext() );
		}

		List<AbstractLuceneIndexSchemaFieldNode> staticChildren = new ArrayList<>();
		LuceneIndexSchemaObjectFieldNode node = new LuceneIndexSchemaObjectFieldNode(
				parentNode, relativeFieldName, inclusion, storage, multiValued, staticChildren
		);

		staticChildrenForParent.add( node );
		collector.collectObjectFieldNode( absoluteFieldPath, node );

		reference.setSchemaNode( node );

		contributeChildren( node, collector, staticChildren );
	}

	@Override
	public LuceneIndexSchemaRootNodeBuilder getRootNodeBuilder() {
		return parent.getRootNodeBuilder();
	}

	@Override
	String getAbsolutePath() {
		return absoluteFieldPath;
	}
}
