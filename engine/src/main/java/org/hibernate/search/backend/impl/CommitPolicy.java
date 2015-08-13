/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.exception.impl.ErrorContextBuilder;

/**
 * Policy for committing changesets.
 *
 * Implementations of this interface will decide
 * the commit strategy based of three backend related events: after a changeset is applied,
 * when an explicit flush is requested and when backend closing
 *
 * @author gustavonalle
 */
public interface CommitPolicy {

	/**
	 * A changeset was applied to the index
	 * @param someFailureHappened true if any failure happened
	 * @param streaming true if changesets are part of a stream of operations
	 */
	void onChangeSetApplied(boolean someFailureHappened, boolean streaming);

	/**
	 * An explicit flush was requested
	 */
	void onFlush();

	/**
	 * Backend shutting down
	 */
	void onClose();

	/**
	 * Obtain the IndexWriter
	 * @return the {@link IndexWriter}
	 */
	IndexWriter getIndexWriter();

	/**
	 * Obtain the index writer
	 * @param errorContextBuilder the {@link ErrorContextBuilder}
	 * @return the {@link IndexWriter}
	 */
	IndexWriter getIndexWriter(ErrorContextBuilder errorContextBuilder);

}
