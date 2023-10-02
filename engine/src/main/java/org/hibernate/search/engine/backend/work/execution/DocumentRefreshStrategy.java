/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.work.execution;

/**
 * Defines how to handle index refreshes after a document is written to the index,
 * i.e. whether changes should be searchable immediately or not.
 */
public enum DocumentRefreshStrategy {

	/**
	 * After a change to an indexed document,
	 * simply let the global index refresh policy follow its course,
	 * without waiting for anything.
	 * <p>
	 * The updated document may be searchable immediately after the document is updated,
	 * or after a short period of time,
	 * depending on the backend and index.
	 */
	NONE,
	/**
	 * After a change to an indexed document,
	 * force a refresh of the corresponding shard to make the updated document searchable immediately,
	 * and wait for that refresh to finish.
	 */
	FORCE

}
