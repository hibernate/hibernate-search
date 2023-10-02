/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierLoader;

public class StandalonePojoMassIdentifierLoader implements PojoMassIdentifierLoader {

	private final MassIdentifierLoader delegate;

	public StandalonePojoMassIdentifierLoader(MassIdentifierLoader delegate) {
		this.delegate = delegate;
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public long totalCount() {
		return delegate.totalCount();
	}

	@Override
	public void loadNext() throws InterruptedException {
		delegate.loadNext();
	}
}
