/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import java.util.List;

import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntityLoader;
import org.hibernate.search.util.common.impl.Closer;

public class StandalonePojoMassEntityLoader<I> implements PojoMassEntityLoader<I> {

	private final StandalonePojoLoadingSessionContext session;
	private final MassEntityLoader<I> delegate;

	public StandalonePojoMassEntityLoader(StandalonePojoLoadingSessionContext session, MassEntityLoader<I> delegate) {
		this.session = session;
		this.delegate = delegate;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( MassEntityLoader::close, delegate );
			closer.push( StandalonePojoLoadingSessionContext::close, session );
		}
	}

	@Override
	public void load(List<I> identifiers) throws InterruptedException {
		delegate.load( identifiers );
	}

}
