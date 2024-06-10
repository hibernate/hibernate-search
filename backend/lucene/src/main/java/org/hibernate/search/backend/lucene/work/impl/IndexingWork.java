/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;


public interface IndexingWork<T> {

	T execute(IndexingWorkExecutionContext context);

	Object getInfo();

	/**
	 * @return A string that will be used to route the work to a specific queue.
	 * Never {@code null}.
	 * Works that must be executed in the same relative order they were submitted in
	 * (i.e. works pertaining to the same document) should return the same string.
	 */
	String getQueuingKey();

}
