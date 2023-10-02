/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import java.util.List;

import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntitySink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntitySink;

public class StandalonePojoMassEntitySink<E> implements MassEntitySink<E> {

	private final PojoMassEntitySink<E> delegate;

	public StandalonePojoMassEntitySink(PojoMassEntitySink<E> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void accept(List<? extends E> batch) throws InterruptedException {
		delegate.accept( batch );
	}

}
