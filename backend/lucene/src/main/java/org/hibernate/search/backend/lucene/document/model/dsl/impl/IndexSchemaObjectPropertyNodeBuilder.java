/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexObjectFieldAccessor;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;

class IndexSchemaObjectPropertyNodeBuilder extends AbstractIndexSchemaNodeBuilder
		implements LuceneIndexSchemaNodeContributor {

	private final DeferredInitializationIndexObjectFieldAccessor accessor =
			new DeferredInitializationIndexObjectFieldAccessor();

	private final String absolutePath;

	private ObjectFieldStorage storage = ObjectFieldStorage.DEFAULT;

	IndexSchemaObjectPropertyNodeBuilder(String parentPath, String relativeName) {
		this.absolutePath = LuceneFields.compose( parentPath, relativeName );
	}

	@Override
	public String getAbsolutePath() {
		return absolutePath;
	}

	public DeferredInitializationIndexObjectFieldAccessor getAccessor() {
		return accessor;
	}

	public void setStorage(ObjectFieldStorage storage) {
		this.storage = storage;
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode) {
		LuceneIndexSchemaObjectNode node = new LuceneIndexSchemaObjectNode( parentNode, absolutePath, storage );
		collector.collectObjectNode( absolutePath, node );

		accessor.initialize( new LuceneIndexObjectFieldAccessor( node, storage ) );

		contributeChildren( node, collector );
	}
}
