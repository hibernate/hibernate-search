/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Iterator;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.spi.HighlightProjectionBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.AbstractStubSearchQueryElementFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexNodeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public class StubFieldHighlightProjection<T, A> extends StubSearchProjection<T> {
	private final String fieldPath;
	private final String highlighterName;
	private final ProjectionCollector<Object, String, A, T> collector;

	public StubFieldHighlightProjection(String fieldPath, String highlighterName,
			ProjectionCollector<Object, String, A, T> collector) {
		this.fieldPath = fieldPath;
		this.highlighterName = highlighterName;
		this.collector = collector;
	}

	@Override
	public Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		return projectionFromIndex.next();
	}

	@Override
	@SuppressWarnings("unchecked")
	public T transform(LoadingResult<?> loadingResult, Object extractedData, StubSearchProjectionContext context) {
		return collector.finish( (A) extractedData );
	}

	@Override
	protected String typeName() {
		return "highlight";
	}

	@Override
	protected void toNode(StubProjectionNode.Builder self) {
		self.attribute( "fieldPath", fieldPath );
		self.attribute( "highlighterName", highlighterName );
	}

	public static class Factory extends AbstractStubSearchQueryElementFactory<HighlightProjectionBuilder> {
		@Override
		public HighlightProjectionBuilder create(StubSearchIndexScope<?> scope,
				StubSearchIndexNodeContext node) {
			return new Builder( node.toValueField().absolutePath() );
		}
	}

	static class Builder extends HighlightProjectionBuilder {
		Builder(String fieldPath) {
			super( fieldPath );
		}

		@Override
		public <V> SearchProjection<V> build(ProjectionCollector.Provider<String, V> collectorProvider) {
			return new StubFieldHighlightProjection<>( path, highlighterName, collectorProvider.get() );
		}
	}
}
