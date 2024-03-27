/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.concurrent.CompletionStage;

import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;

public class StubBackendFactory implements BackendFactory {

	private final StubBackendBehavior behavior;
	private final CompletionStage<BackendMappingHandle> mappingHandlePromise;

	public StubBackendFactory(StubBackendBehavior behavior,
			CompletionStage<BackendMappingHandle> mappingHandlePromise) {
		this.behavior = behavior;
		this.mappingHandlePromise = mappingHandlePromise;
	}

	@Override
	public BackendImplementor create(EventContext eventContext, BackendBuildContext context,
			ConfigurationPropertySource propertySource) {
		return new StubBackend( eventContext, context, behavior, mappingHandlePromise, context.timingSource(), propertySource );
	}
}
