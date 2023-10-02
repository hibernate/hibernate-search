/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub;

import static org.assertj.core.api.Assertions.fail;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.EventContextElement;

public class StubUnusedContextualFailureCollector implements ContextualFailureCollector {
	@Override
	public boolean hasFailure() {
		return false;
	}

	@Override
	public void add(Throwable t) {
		fail( "Unexpected call to add(" + t + ")" );
	}

	@Override
	public void add(String failureMessage) {
		fail( "Unexpected call to add(" + failureMessage + ")" );
	}

	@Override
	public ContextualFailureCollector withContext(EventContext context) {
		return fail( "Unexpected call to withContext(" + context + ")" );
	}

	@Override
	public ContextualFailureCollector withContext(EventContextElement contextElement) {
		return fail( "Unexpected call to withContext(" + contextElement + ")" );
	}

	@Override
	public EventContext eventContext() {
		return fail( "Unexpected call to eventContext()" );
	}
}
