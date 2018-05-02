/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.document.model.dsl.impl.LuceneIndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.query.impl.SearchBackendContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.util.impl.common.LoggerFactory;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

/**
 * @author Guillaume Smet
 */
public class LuceneDirectoryIndexManagerBuilder implements IndexManagerBuilder<LuceneRootDocumentBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexingBackendContext indexingBackendContext;
	private final SearchBackendContext searchBackendContext;

	private final String normalizedIndexName;
	private final LuceneIndexSchemaRootNodeBuilder schemaRootNodeBuilder = new LuceneIndexSchemaRootNodeBuilder();

	private final BuildContext buildContext;
	private final ConfigurationPropertySource propertySource;

	public LuceneDirectoryIndexManagerBuilder(IndexingBackendContext indexingBackendContext,
			SearchBackendContext searchBackendContext,
			String normalizedIndexName,
			BuildContext buildContext, ConfigurationPropertySource propertySource) {
		this.indexingBackendContext = indexingBackendContext;
		this.searchBackendContext = searchBackendContext;
		this.normalizedIndexName = normalizedIndexName;
		this.buildContext = buildContext;
		this.propertySource = propertySource;
	}

	@Override
	public IndexSchemaRootNodeBuilder getSchemaRootNodeBuilder() {
		return schemaRootNodeBuilder;
	}

	@Override
	public LuceneDirectoryIndexManager build() {
		LuceneIndexModel model = new LuceneIndexModel( normalizedIndexName, schemaRootNodeBuilder );

		return new LuceneDirectoryIndexManager(
				indexingBackendContext, searchBackendContext, normalizedIndexName, model, createIndexWriter( model )
		);
	}

	private IndexWriter createIndexWriter(LuceneIndexModel model) {
		// FIXME properly close all the resources, this will be pretty convoluted and we will likely drop this code
		// altogether so let's be naive for now

		try {
			IndexWriterConfig indexWriterConfig = new IndexWriterConfig( model.getScopedAnalyzer() );
			Directory directory = indexingBackendContext.createDirectory( normalizedIndexName );
			return new IndexWriter( directory, indexWriterConfig );
		}
		catch (IOException e) {
			throw log.unableToCreateIndexWriter(
					indexingBackendContext.getBackendImplementor(), model.getIndexName(), e
			);
		}
	}
}
