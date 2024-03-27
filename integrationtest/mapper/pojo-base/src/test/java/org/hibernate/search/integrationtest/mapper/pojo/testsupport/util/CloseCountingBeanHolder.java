/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.util;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

public class CloseCountingBeanHolder<T> implements BeanHolder<T> {
	private final T bean;
	private final StaticCounters.Key closeKey;
	private boolean closed = false;

	public CloseCountingBeanHolder(T bean, StaticCounters.Key closeKey) {
		this.bean = bean;
		this.closeKey = closeKey;
	}

	@Override
	public T get() {
		return bean;
	}

	@Override
	public void close() {
		/*
		 * This is important so that multiple calls to close on a single bridge
		 * won't be interpreted as closing multiple objects in test assertions.
		 */
		if ( closed ) {
			return;
		}
		StaticCounters.get().increment( closeKey );
		closed = true;
	}
}
