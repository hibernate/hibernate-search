/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Iterator;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

abstract class AbstractStubPassThroughProjection<T> extends StubSearchProjection<T> {
	@Override
	public final Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		return projectionFromIndex.next();
	}

	@SuppressWarnings("unchecked")
	@Override
	public final T transform(LoadingResult<?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		return (T) extractedData;
	}

	@Override
	protected final void toNode(StubProjectionNode.Builder self) {
		// Nothing to do
	}
}
