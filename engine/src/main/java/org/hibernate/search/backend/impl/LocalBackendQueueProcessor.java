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
 * A {@link BackendQueueProcessor} which applies given index changes locally to the corresponding {@link IndexManager}.
 *
 * @author Gunnar Morling
 */
public class LocalBackendQueueProcessor implements BackendQueueProcessor {

	private IndexManager indexManager;

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
