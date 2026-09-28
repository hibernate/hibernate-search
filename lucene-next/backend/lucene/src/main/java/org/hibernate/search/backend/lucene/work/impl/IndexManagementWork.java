package org.hibernate.search.backend.lucene.work.impl;


public interface IndexManagementWork<T> {

	T execute(IndexManagementWorkExecutionContext context);

	Object getInfo();

}
