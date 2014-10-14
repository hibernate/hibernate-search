/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import org.apache.lucene.index.IndexWriter;

/**
 * Commit policy that will always commit on every changeset and when flush is called
 *
 * @author gustavonalle
 */
public final class PerChangeSetCommitPolicy extends AbstractCommitPolicy {

	public PerChangeSetCommitPolicy(IndexWriterHolder indexWriterHolder) {
		super( indexWriterHolder );
	}

	@Override
	public void onChangeSetApplied(boolean someFailureHappened, boolean streaming) {
		if ( someFailureHappened ) {
			indexWriterHolder.forceLockRelease();
		}
		else {
			if ( !streaming ) {
				indexWriterHolder.commitIndexWriter();
			}
		}
	}

	@Override
	public void onFlush() {
		indexWriterHolder.commitIndexWriter();
	}

	@Override
	public IndexWriter getIndexWriter() {
		return indexWriterHolder.getIndexWriter();
	}

}
