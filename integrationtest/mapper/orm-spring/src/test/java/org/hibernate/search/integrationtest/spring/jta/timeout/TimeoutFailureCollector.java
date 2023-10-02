/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.jta.timeout;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;

public class TimeoutFailureCollector implements FailureHandler {

	public static final Set<Throwable> EXCEPTIONS = ConcurrentHashMap.newKeySet();

	@Override
	public void handle(FailureContext context) {
		EXCEPTIONS.add( context.throwable() );
	}

	@Override
	public void handle(EntityIndexingFailureContext context) {
		// do-nothing
	}
}
