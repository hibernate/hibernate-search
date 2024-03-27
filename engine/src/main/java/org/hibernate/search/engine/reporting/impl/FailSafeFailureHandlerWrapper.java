/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.reporting.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class FailSafeFailureHandlerWrapper implements FailureHandler {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
			log.failureInFailureHandler( t );
		}
	}

	@Override
	public void handle(EntityIndexingFailureContext context) {
		try {
			delegate.handle( context );
		}
		catch (Throwable t) {
			log.failureInFailureHandler( t );
		}
	}

	@Override
	public long failureFloodingThreshold() {
		try {
			return delegate.failureFloodingThreshold();
		}
		catch (Throwable t) {
			log.failureInFailureHandler( t );
			return FailureHandler.super.failureFloodingThreshold();
		}
	}
}
