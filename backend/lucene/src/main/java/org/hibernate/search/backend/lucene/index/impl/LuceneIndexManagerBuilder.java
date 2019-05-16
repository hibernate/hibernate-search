/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterHolder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.document.model.dsl.impl.LuceneIndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.query.impl.SearchBackendContext;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.impl.SuppressingCloser;

import org.apache.lucene.store.Directory;

/**
 * @author Guillaume Smet
 */
public class LuceneIndexManagerBuilder implements IndexManagerBuilder<LuceneRootDocumentBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexingBackendContext indexingBackendContext;
	private final SearchBackendContext searchBackendContext;

	private final String indexName;
	private final LuceneIndexSchemaRootNodeBuilder schemaRootNodeBuilder;

	public LuceneIndexManagerBuilder(IndexingBackendContext indexingBackendContext,
			SearchBackendContext searchBackendContext,
			String indexName,
			LuceneIndexSchemaRootNodeBuilder schemaRootNodeBuilder) {
		this.indexingBackendContext = indexingBackendContext;
		this.searchBackendContext = searchBackendContext;
		this.indexName = indexName;
		this.schemaRootNodeBuilder = schemaRootNodeBuilder;
	}

	@Override
	public void closeOnFailure() {
		// Nothing to do
	}

	@Override
	public IndexSchemaRootNodeBuilder getSchemaRootNodeBuilder() {
		return schemaRootNodeBuilder;
	}

	@Override
	public LuceneIndexManagerImpl build() {
		LuceneIndexModel model = null;
		IndexWriterHolder indexWriterHolder = null;
		try {
			model = schemaRootNodeBuilder.build( indexName );
			indexWriterHolder = createIndexWriterHolder( model );
			return new LuceneIndexManagerImpl(
					indexingBackendContext, searchBackendContext, indexName, model, indexWriterHolder
			);
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( model )
					.push( IndexWriterHolder::closeIndexWriter, indexWriterHolder );
			throw e;
		}
	}

	private IndexWriterHolder createIndexWriterHolder(LuceneIndexModel model) {
		Directory directory;
		try {
			directory = indexingBackendContext.createDirectory( indexName );
		}
		catch (IOException | RuntimeException e) {
			throw log.unableToCreateIndexDirectory( getEventContext(), e );
		}
		try {
			return indexingBackendContext.createIndexWriterHolder(
					model.getIndexName(), directory, model.getScopedAnalyzer()
			);
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( directory );
			throw e;
		}
	}

	private EventContext getEventContext() {
		return indexingBackendContext.getEventContext().append(
				EventContexts.fromIndexName( indexName )
		);
	}
}
