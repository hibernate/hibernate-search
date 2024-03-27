/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Iterator;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class StubReferenceProjection<T> extends StubSearchProjection<T> {

	@SuppressWarnings("rawtypes")
	private static final StubSearchProjection INSTANCE = new StubReferenceProjection();

	@SuppressWarnings("unchecked")
	public static <T> StubReferenceProjection<T> get() {
		return (StubReferenceProjection<T>) INSTANCE;
	}

	private StubReferenceProjection() {
	}

	@Override
	public Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		return projectionFromIndex.next();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		DocumentReference data = (DocumentReference) extractedData;
		return (T) loadingResult.convertReference( data );
	}

	@Override
	protected String typeName() {
		return "reference";
	}

	@Override
	protected void toNode(StubProjectionNode.Builder self) {
		// Nothing to do
	}
}
