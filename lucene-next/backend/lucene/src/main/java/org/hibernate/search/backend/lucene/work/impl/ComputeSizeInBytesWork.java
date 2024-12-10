/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.impl;


public class ComputeSizeInBytesWork implements IndexManagementWork<Long> {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Long execute(IndexManagementWorkExecutionContext context) {
		return context.getIndexAccessor().computeSizeInBytes();
	}

	@Override
	public Object getInfo() {
		return this;
	}
}
