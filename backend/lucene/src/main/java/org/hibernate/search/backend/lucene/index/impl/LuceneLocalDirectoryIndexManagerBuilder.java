/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneRootIndexSchemaCollectorImpl;
import org.hibernate.search.backend.lucene.impl.LuceneLocalDirectoryBackend;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * @author Guillaume Smet
 */
public class LuceneLocalDirectoryIndexManagerBuilder implements IndexManagerBuilder<LuceneRootDocumentBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneLocalDirectoryBackend backend;
	private final String normalizedIndexName;
	private final BuildContext context;
	private final ConfigurationPropertySource propertySource;

	private final LuceneRootIndexSchemaCollectorImpl collector = new LuceneRootIndexSchemaCollectorImpl();

	public LuceneLocalDirectoryIndexManagerBuilder(LuceneLocalDirectoryBackend backend, String normalizedIndexName,
			BuildContext context, ConfigurationPropertySource propertySource) {
		this.backend = backend;
		this.normalizedIndexName = normalizedIndexName;
		this.context = context;
		this.propertySource = propertySource;
	}

	@Override
	public IndexSchemaCollector getSchemaCollector() {
		return collector;
	}

	@Override
	public LuceneLocalDirectoryIndexManager build() {
		LuceneIndexModel model = new LuceneIndexModel( normalizedIndexName, collector );

		return new LuceneLocalDirectoryIndexManager( backend, normalizedIndexName, model, createIndexWriter( model ) );
	}

	private IndexWriter createIndexWriter(LuceneIndexModel model) {
		Path directoryPath = backend.getRootDirectory().resolve( normalizedIndexName );
		initializeIndexDirectory( directoryPath );

		// FIXME properly close all the resources, this will be pretty convoluted and we will likely drop this code
		// altogether so let's be naive for now

		try {
			IndexWriterConfig indexWriterConfig = new IndexWriterConfig( model.getScopedAnalyzer() );
			Directory directory = new MMapDirectory( directoryPath );

			return new IndexWriter( directory, indexWriterConfig );
		}
		catch (IOException e) {
			throw log.unableToCreateIndexWriter( backend, model.getIndexName(), directoryPath, e );
		}
	}

	private void initializeIndexDirectory(Path indexDirectory) {
		if ( Files.exists( indexDirectory ) ) {
			if ( !Files.isDirectory( indexDirectory ) || !Files.isWritable( indexDirectory ) ) {
				throw log.localDirectoryIndexRootDirectoryNotWritableDirectory( backend, indexDirectory );
			}
		}
		else {
			try {
				Files.createDirectories( indexDirectory );
			}
			catch (Exception e) {
				throw log.unableToCreateIndexRootDirectoryForLocalDirectoryBackend( backend, indexDirectory, e );
			}
		}
	}
}
