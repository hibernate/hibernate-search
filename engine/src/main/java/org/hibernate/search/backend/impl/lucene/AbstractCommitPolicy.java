/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.impl.CommitPolicy;
import org.hibernate.search.exception.impl.ErrorContextBuilder;

/**
 * Base class for {@link org.hibernate.search.backend.impl.CommitPolicy}
 *
 * @author gustavonalle
 */
public abstract class AbstractCommitPolicy implements CommitPolicy {

	protected final IndexWriterHolder indexWriterHolder;

	public AbstractCommitPolicy(IndexWriterHolder indexWriterHolder) {
		this.indexWriterHolder = indexWriterHolder;
	}

	public IndexWriter getIndexWriter() {
		return indexWriterHolder.getIndexWriter();
	}

	@Override
	public IndexWriter getIndexWriter(ErrorContextBuilder errorContextBuilder) {
		return indexWriterHolder.getIndexWriter( errorContextBuilder );
	}

	@Override
	public void onClose() {
	}
}
