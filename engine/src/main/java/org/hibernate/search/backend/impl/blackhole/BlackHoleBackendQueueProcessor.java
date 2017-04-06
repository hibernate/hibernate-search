/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.blackhole;

import java.util.List;
import java.util.Properties;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A queue processor for the {@link BlackHoleBackend}.
 *
 * @author Sanne Grinovero
 */
public class BlackHoleBackendQueueProcessor implements BackendQueueProcessor {

	private static final Log log = LoggerFactory.make();

	@Override
	public void initialize(Properties props, WorkerBuildContext context, IndexManager indexManager) {
		// no-op
		log.initializedBlackholeBackend();
	}

	@Override
	public void close() {
		// no-op
		log.closedBlackholeBackend();
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		// no-op
		log.debug( "Discarding a list of LuceneWork" );
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		// no-op
		log.debug( "Discarding a single LuceneWork" );
	}
}
