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
