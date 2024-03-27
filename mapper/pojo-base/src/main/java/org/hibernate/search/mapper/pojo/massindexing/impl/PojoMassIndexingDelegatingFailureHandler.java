/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEntityFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;

public class PojoMassIndexingDelegatingFailureHandler implements MassIndexingFailureHandler {

	private final FailureHandler delegate;

	public PojoMassIndexingDelegatingFailureHandler(FailureHandler delegate) {
		this.delegate = delegate;
	}

	@Override
	public void handle(MassIndexingFailureContext context) {
		FailureContext.Builder builder = FailureContext.builder();
		builder.throwable( context.throwable() );
		builder.failingOperation( context.failingOperation() );
		delegate.handle( builder.build() );
	}

	@Override
	public void handle(MassIndexingEntityFailureContext context) {
		EntityIndexingFailureContext.Builder builder = EntityIndexingFailureContext.builder();
		builder.throwable( context.throwable() );
		builder.failingOperation( context.failingOperation() );
		for ( EntityReference entityReference : context.failingEntityReferences() ) {
			builder.failingEntityReference( entityReference );
		}
		delegate.handle( builder.build() );
	}

	@Override
	public long failureFloodingThreshold() {
		return delegate.failureFloodingThreshold();
	}
}
