/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.blackhole;

import org.hibernate.search.backend.spi.Backend;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * This backend does not do anything: the Documents are not
 * sent to any index but are discarded.
 * Useful to identify the bottleneck in indexing performance problems,
 * fully disabling the backend system but still building the Documents
 * needed to update an index (loading data from DB).
 *
 * @author Sanne Grinovero
 */
public class BlackHoleBackend implements Backend {

	public static final BlackHoleBackend INSTANCE = new BlackHoleBackend();

	private final BackendQueueProcessor queueProcessor = new BlackHoleBackendQueueProcessor();

	private BlackHoleBackend() {
		// Not allowed
	}

	@Override
	public BackendQueueProcessor createQueueProcessor(IndexManager indexManager, WorkerBuildContext context) {
		return queueProcessor;
	}
}
