package org.hibernate.search.backend.lucene.work.impl;


public interface ReadWork<T> {

	T execute(ReadWorkExecutionContext context);

}
