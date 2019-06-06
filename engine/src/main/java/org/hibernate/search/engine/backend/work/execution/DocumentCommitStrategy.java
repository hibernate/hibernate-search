/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.work.execution;

public enum DocumentCommitStrategy {

	/**
	 * After a change to an indexed document,
	 * simply let the global index commit policy follow its course,
	 * without waiting for or forcing a commit.
	 * <p>
	 * If the backend fails before committing the changes to the document, the changes may be lost.
	 */
	NONE,
	/**
	 * After a change to an indexed document,
	 * force a commit of the relevant shard and wait for that commit to finish,
	 * to ensure changes are persisted to disk.
	 */
	FORCE

}
