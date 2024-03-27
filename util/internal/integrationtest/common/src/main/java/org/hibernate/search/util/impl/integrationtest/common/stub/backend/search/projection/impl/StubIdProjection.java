/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Iterator;

import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class StubIdProjection<I> extends StubSearchProjection<I> {

	private final Class<I> requestedIdentifierType;
	private final ProjectionConverter<String, ? extends I> converter;

	StubIdProjection(Class<I> requestedIdentifierType, ProjectionConverter<String, ? extends I> converter) {
		this.requestedIdentifierType = requestedIdentifierType;
		this.converter = converter;
	}

	@Override
	public Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		return projectionFromIndex.next();
	}

	@SuppressWarnings("unchecked")
	@Override
	public I transform(LoadingResult<?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		String documentId = (String) extractedData;

		context.fromDocumentValueConvertContext();
		return converter.fromDocumentValue( documentId, context.fromDocumentValueConvertContext() );
	}

	@Override
	protected String typeName() {
		return "id";
	}

	@Override
	protected void toNode(StubProjectionNode.Builder self) {
		self.attribute( "requestedIdentifierType", requestedIdentifierType );
		self.attribute( "converter", converter );
	}
}
