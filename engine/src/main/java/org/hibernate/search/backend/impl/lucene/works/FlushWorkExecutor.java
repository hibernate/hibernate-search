/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene.works;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexWriterDelegate;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Performs a flush of all pending changes.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 * @see IndexUpdateVisitor
 * @see LuceneWorkExecutor
 * @since 4.1
 */
class FlushWorkExecutor implements LuceneWorkExecutor {

	private static final Log log = LoggerFactory.make();

	private final Workspace workspace;

	FlushWorkExecutor(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public void performWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor) {
		log.debug( "performing FlushWorkDelegate" );
		workspace.flush();
	}

}
