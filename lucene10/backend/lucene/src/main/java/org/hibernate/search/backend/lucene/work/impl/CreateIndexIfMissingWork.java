/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;


public class CreateIndexIfMissingWork implements IndexManagementWork<Void> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Void execute(IndexManagementWorkExecutionContext context) {
		context.getIndexAccessor().createIndexIfMissing();
		return null;
	}

	@Override
	public Object getInfo() {
		return this;
	}
}
