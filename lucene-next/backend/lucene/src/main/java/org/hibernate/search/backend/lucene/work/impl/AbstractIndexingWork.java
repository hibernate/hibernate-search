/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;

public abstract class AbstractIndexingWork<T> implements IndexingWork<T> {

	protected final String workType;

	AbstractIndexingWork(String workType) {
		this.workType = workType;
	}

	@Override
	public Object getInfo() {
		// TODO extract immutable work relevant info. We need to think about it. See HSEARCH-3110.
		return this;
	}
}
