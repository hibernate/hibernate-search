/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;


import org.hibernate.search.backend.spi.Backend;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * A {@link Backend} which applies given index changes locally to the corresponding {@link IndexManager}.
 *
 * @author Gunnar Morling
 */
public class LocalBackend implements Backend {

	public static final LocalBackend INSTANCE = new LocalBackend();


	private LocalBackend() {
		// Not allowed
	}

	@Override
	public BackendQueueProcessor createQueueProcessor(IndexManager indexManager, WorkerBuildContext context) {
		return new LocalBackendQueueProcessor( indexManager );
	}

}
