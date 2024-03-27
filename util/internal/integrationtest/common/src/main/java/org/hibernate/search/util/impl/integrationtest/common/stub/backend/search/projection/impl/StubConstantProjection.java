/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Iterator;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

class StubConstantProjection<T> extends StubSearchProjection<T> {

	private final T value;

	StubConstantProjection(T value) {
		this.value = value;
	}

	@Override
	public Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		return value;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		return (T) extractedData;
	}

	@Override
	protected String typeName() {
		return "constant";
	}

	@Override
	protected void toNode(StubProjectionNode.Builder self) {
		self.attribute( "value", value );
	}
}
