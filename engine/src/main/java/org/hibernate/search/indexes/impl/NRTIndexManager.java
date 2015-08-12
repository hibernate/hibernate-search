/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import java.util.Properties;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.backend.impl.lucene.NRTWorkspaceImpl;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.DirectoryBasedReaderProvider;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * IndexManager implementation taking advantage of the Near-Real-Time
 * features of Lucene.
 * When using this work mode the IndexWriter does not need a full
 * flush at the end of each operation: new IndexReaders are able to
 * inspect the unflushed changes still pending in the IndexWriter buffers.
 *
 * This improves write performance as the IndexWriter doesn't need
 * to commit as often, but has two limitations:
 * <ul>
 * <li>unsaved index data might be lost in case of crashes</li>
 * <li>is not useful for non-local (clustered) backends</li>
 * </ul>
 *
 * @since 4.0
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class NRTIndexManager extends DirectoryBasedIndexManager {

	private static final Log log = LoggerFactory.make();
	private NRTWorkspaceImpl nrtWorkspace;

	@Override
	protected BackendQueueProcessor createBackend(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		String backend = cfg.getProperty( Environment.WORKER_BACKEND );
		if ( backend != null ) {
			log.ignoringBackendOptionForIndex( indexName, "near-real-time" );
		}
		LuceneBackendQueueProcessor backendQueueProcessor = new LuceneBackendQueueProcessor();
		nrtWorkspace = new NRTWorkspaceImpl( this, buildContext, cfg );
		backendQueueProcessor.setCustomWorkspace( nrtWorkspace );
		backendQueueProcessor.initialize( cfg, buildContext, this );
		return backendQueueProcessor;
	}

	@Override
	protected DirectoryBasedReaderProvider createIndexReader(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		return nrtWorkspace;
	}

}
