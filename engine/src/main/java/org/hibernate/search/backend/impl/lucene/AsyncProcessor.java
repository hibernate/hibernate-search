/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.List;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;


/**
 * AsyncProcessor.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 * @since 5.0
 */
public class AsyncProcessor implements BlockProcessor {

	private volatile LuceneBackendResources resources;

	public AsyncProcessor(LuceneBackendResources resources) {
		this.resources = resources;
	}

	@Override
	public void shutdown() {
		//no-op
	}

	@Override
	public void submit(List<LuceneWork> workList, IndexingMonitor monitor) {
		LuceneBackendQueueTask luceneBackendQueueProcessor = new LuceneBackendQueueTask(
				workList,
				resources,
				monitor
		);
		resources.getQueueingExecutor().execute( luceneBackendQueueProcessor );
	}

	@Override
	public void updateResources(LuceneBackendResources resources) {
		this.resources = resources;
	}

}
