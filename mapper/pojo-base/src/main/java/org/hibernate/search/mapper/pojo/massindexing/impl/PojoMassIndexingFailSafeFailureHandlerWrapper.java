/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEntityFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoMassIndexingFailSafeFailureHandlerWrapper implements MassIndexingFailureHandler {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final MassIndexingFailureHandler delegate;
	private final boolean failFast;

	public PojoMassIndexingFailSafeFailureHandlerWrapper(MassIndexingFailureHandler delegate, boolean failFast) {
		this.delegate = delegate;
		this.failFast = failFast;
	}

	@Override
	public void handle(MassIndexingFailureContext context) {
		try {
			delegate.handle( context );
		}
		catch (Throwable t) {
			log.failureInMassIndexingFailureHandler( t );
		}
		finally {
			failFastIfNeeded();
		}
	}

	@Override
	public void handle(MassIndexingEntityFailureContext context) {
		try {
			delegate.handle( context );
		}
		catch (Throwable t) {
			log.failureInMassIndexingFailureHandler( t );
		}
		finally {
			failFastIfNeeded();
		}
	}

	@Override
	public long failureFloodingThreshold() {
		try {
			return delegate.failureFloodingThreshold();
		}
		catch (Throwable t) {
			log.failureInMassIndexingFailureHandler( t );
			return MassIndexingFailureHandler.super.failureFloodingThreshold();
		}
	}

	private void failFastIfNeeded() {
		if ( failFast ) {
			throw log.massIndexerFailFast();
		}
	}
}
