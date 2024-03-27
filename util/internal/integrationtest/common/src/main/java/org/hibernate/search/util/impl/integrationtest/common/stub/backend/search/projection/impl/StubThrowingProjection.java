/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Iterator;
import java.util.function.Supplier;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.common.SearchException;

class StubThrowingProjection<T> extends StubSearchProjection<T> {

	private final Supplier<SearchException> exceptionSupplier;

	StubThrowingProjection(Supplier<SearchException> exceptionSupplier) {
		this.exceptionSupplier = exceptionSupplier;
	}

	@Override
	public Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		throw exceptionSupplier.get();
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		throw exceptionSupplier.get();
	}

	@Override
	protected String typeName() {
		return "throwing";
	}

	@Override
	public void toNode(StubProjectionNode.Builder self) {
		// Nothing to do
	}
}
