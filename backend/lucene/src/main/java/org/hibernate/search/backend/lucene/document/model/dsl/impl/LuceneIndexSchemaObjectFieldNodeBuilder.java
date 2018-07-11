/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.spi.DeferredInitializationIndexObjectFieldAccessor;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexObjectFieldAccessor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.engine.logging.spi.FailureContext;
import org.hibernate.search.engine.logging.spi.FailureContexts;

class LuceneIndexSchemaObjectFieldNodeBuilder extends AbstractLuceneIndexSchemaObjectNodeBuilder
		implements IndexSchemaObjectFieldNodeBuilder, LuceneIndexSchemaNodeContributor {

	private final AbstractLuceneIndexSchemaObjectNodeBuilder parent;
	private final String absoluteFieldPath;
	private final ObjectFieldStorage storage;

	private final DeferredInitializationIndexObjectFieldAccessor accessor =
			new DeferredInitializationIndexObjectFieldAccessor();

	LuceneIndexSchemaObjectFieldNodeBuilder(AbstractLuceneIndexSchemaObjectNodeBuilder parent,
			String relativeFieldName, ObjectFieldStorage storage) {
		this.parent = parent;
		this.absoluteFieldPath = LuceneFields.compose( parent.getAbsolutePath(), relativeFieldName );
		this.storage = storage;
	}

	@Override
	public FailureContext getFailureContext() {
		return FailureContext.create(
				getRootNodeBuilder().getIndexFailureContextElement(),
				FailureContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public IndexObjectFieldAccessor getAccessor() {
		return accessor;
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode) {
		LuceneIndexSchemaObjectNode node = new LuceneIndexSchemaObjectNode( parentNode, absoluteFieldPath, storage );
		collector.collectObjectNode( absoluteFieldPath, node );

		accessor.initialize( new LuceneIndexObjectFieldAccessor( node, storage ) );

		contributeChildren( node, collector );
	}

	@Override
	LuceneIndexSchemaRootNodeBuilder getRootNodeBuilder() {
		return parent.getRootNodeBuilder();
	}

	@Override
	String getAbsolutePath() {
		return absoluteFieldPath;
	}
}
