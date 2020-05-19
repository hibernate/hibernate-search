/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntryFactory;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.backend.lucene.document.model.dsl.impl.LuceneIndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.util.common.impl.SuppressingCloser;


public class LuceneIndexManagerBuilder implements IndexManagerBuilder {

	private final IndexManagerBackendContext backendContext;

	private final String indexName;
	private final LuceneIndexSchemaRootNodeBuilder schemaRootNodeBuilder;

	public LuceneIndexManagerBuilder(IndexManagerBackendContext backendContext,
			String indexName,
			LuceneIndexSchemaRootNodeBuilder schemaRootNodeBuilder) {
		this.backendContext = backendContext;
		this.indexName = indexName;
		this.schemaRootNodeBuilder = schemaRootNodeBuilder;
	}

	@Override
	public void closeOnFailure() {
		// Nothing to do
	}

	@Override
	public IndexSchemaRootNodeBuilder schemaRootNodeBuilder() {
		return schemaRootNodeBuilder;
	}

	@Override
	public LuceneIndexManagerImpl build() {
		LuceneIndexModel model = null;
		try {
			model = schemaRootNodeBuilder.build( indexName );
			LuceneIndexEntryFactory indexEntryFactory = backendContext.createLuceneIndexEntryFactory( model );
			return new LuceneIndexManagerImpl(
					backendContext, indexName, model, indexEntryFactory
			);
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( model );
			throw e;
		}
	}
}
