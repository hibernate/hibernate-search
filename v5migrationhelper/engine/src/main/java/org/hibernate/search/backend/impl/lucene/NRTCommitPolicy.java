/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

/**
 * Commit policy for Near Real Time usage of a indexWriter. Will only commit if explicitly requested
 *
 * @author gustavonalle
 */
public final class NRTCommitPolicy extends AbstractCommitPolicy {

	public NRTCommitPolicy(IndexWriterHolder indexWriterHolder) {
		super( indexWriterHolder );
	}

	@Override
	public void onChangeSetApplied(boolean someFailureHappened, boolean streaming) {
		if ( someFailureHappened ) {
			indexWriterHolder.forceLockRelease();
		}
	}

	@Override
	public void onFlush() {
		indexWriterHolder.commitIndexWriter();
	}

}
