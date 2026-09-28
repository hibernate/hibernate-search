package org.hibernate.search.util.impl.integrationtest.common.extension;

public interface ParameterizedCallBehavior<C, T> {
	T execute(C context);
}
