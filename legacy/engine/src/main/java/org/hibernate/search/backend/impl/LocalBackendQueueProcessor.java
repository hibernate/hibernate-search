/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import java.util.List;
import java.util.Properties;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * A queue processor for the {@link LocalBackend}.
 *
 * @author Gunnar Morling
 */
public class LocalBackendQueueProcessor implements BackendQueueProcessor {

	private IndexManager indexManager;

	/**
	 * @deprecated Provided so that passing the LocalBackendQueueProcessor class
	 * as the value of the "backend" configuration option still works, but normally
	 * the "local" string should be used instead, in which case the other constructor
	 * is used.
	 */
	@Deprecated
	public LocalBackendQueueProcessor() {
	}

	public LocalBackendQueueProcessor(IndexManager indexManager) {
		this.indexManager = indexManager;
	}

	/**
	 * @deprecated Provided so that passing the LocalBackendQueueProcessor class
	 * as the value of the "backend" configuration option still works, but normally
	 * the "local" string should be used instead, in which case this method is not used.
	 */
	@Deprecated
	@Override
	public void initialize(Properties props, WorkerBuildContext context, IndexManager indexManager) {
		this.indexManager = indexManager;
	}

	@Override
	public void close() {
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		indexManager.performOperations( workList, monitor );
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		indexManager.performStreamOperation( singleOperation, monitor, false );
	}
}
