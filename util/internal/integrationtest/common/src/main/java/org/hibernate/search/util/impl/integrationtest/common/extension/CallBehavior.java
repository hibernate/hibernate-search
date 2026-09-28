package org.hibernate.search.util.impl.integrationtest.common.extension;

public interface CallBehavior<T> {
	T execute();
}
