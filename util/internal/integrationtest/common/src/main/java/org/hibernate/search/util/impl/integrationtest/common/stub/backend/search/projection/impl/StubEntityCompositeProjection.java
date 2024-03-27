/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Iterator;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class StubEntityCompositeProjection<T> extends StubSearchProjection<T> {

	private final StubSearchProjection<T> delegate;

	public StubEntityCompositeProjection(StubSearchProjection<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	protected String typeName() {
		return "composite";
	}

	@Override
	protected void toNode(StubProjectionNode.Builder self) {
		self.attribute( "delegate", delegate );
	}

	@Override
	public Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		return delegate.extract( projectionHitMapper, projectionFromIndex, context );
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData, StubSearchProjectionContext context) {
		return delegate.transform( loadingResult, extractedData, context );
	}
}
