/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import org.hibernate.search.mapper.pojo.logging.impl.MassIndexingLog;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEntityFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;

public class PojoMassIndexingFailSafeFailureHandlerWrapper implements MassIndexingFailureHandler {

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
			MassIndexingLog.INSTANCE.failureInMassIndexingFailureHandler( t );
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
			MassIndexingLog.INSTANCE.failureInMassIndexingFailureHandler( t );
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
			MassIndexingLog.INSTANCE.failureInMassIndexingFailureHandler( t );
			return MassIndexingFailureHandler.super.failureFloodingThreshold();
		}
	}

	private void failFastIfNeeded() {
		if ( failFast ) {
			throw MassIndexingLog.INSTANCE.massIndexerFailFast();
		}
	}
}
