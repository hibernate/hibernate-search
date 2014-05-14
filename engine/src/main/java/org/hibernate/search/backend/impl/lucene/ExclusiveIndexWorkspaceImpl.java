/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.Properties;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class ExclusiveIndexWorkspaceImpl extends AbstractWorkspaceImpl {

	public ExclusiveIndexWorkspaceImpl(DirectoryBasedIndexManager indexManager, WorkerBuildContext context, Properties cfg) {
		super( indexManager, context, cfg );
	}

	@Override
	public void afterTransactionApplied(boolean someFailureHappened, boolean streaming) {
		if ( someFailureHappened ) {
			writerHolder.forceLockRelease();
		}
		else {
			if ( ! streaming ) {
				writerHolder.commitIndexWriter();
			}
		}
	}

	@Override
	public void flush() {
		writerHolder.commitIndexWriter();
	}

	@Override
	public void notifyWorkApplied(LuceneWork work) {
		incrementModificationCounter();
	}

}
