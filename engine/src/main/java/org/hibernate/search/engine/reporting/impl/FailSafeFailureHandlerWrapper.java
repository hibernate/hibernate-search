/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.reporting.impl;

import static org.hibernate.search.engine.logging.impl.CommonFailureLog.INSTANCE;

import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;

public class FailSafeFailureHandlerWrapper implements FailureHandler {

	private final FailureHandler delegate;

	public FailSafeFailureHandlerWrapper(FailureHandler delegate) {
		this.delegate = delegate;
	}

	@Override
	public void handle(FailureContext context) {
		try {
			delegate.handle( context );
		}
		catch (Throwable t) {
			INSTANCE.failureInFailureHandler( t );
		}
	}

	@Override
	public void handle(EntityIndexingFailureContext context) {
		try {
			delegate.handle( context );
		}
		catch (Throwable t) {
			INSTANCE.failureInFailureHandler( t );
		}
	}

	@Override
	public long failureFloodingThreshold() {
		try {
			return delegate.failureFloodingThreshold();
		}
		catch (Throwable t) {
			INSTANCE.failureInFailureHandler( t );
			return FailureHandler.super.failureFloodingThreshold();
		}
	}
}
