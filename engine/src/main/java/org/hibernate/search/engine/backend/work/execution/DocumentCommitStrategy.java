/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.work.execution;

/**
 * Defines how to handle index commits after a document is written to the index,
 * i.e. whether changes should be committed to disk immediately or not.
 */
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
