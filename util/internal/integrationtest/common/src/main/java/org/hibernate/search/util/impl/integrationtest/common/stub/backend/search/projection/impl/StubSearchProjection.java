/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Iterator;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

public abstract class StubSearchProjection<P> implements SearchProjection<P>, ToStringTreeAppendable {

	@Override
	public final String toString() {
		return toStringTree();
	}

	@Override
	public final void appendTo(ToStringTreeAppender appender) {
		toRootNode().appendTo( appender );
	}

	public abstract Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context);

	public abstract P transform(LoadingResult<?> loadingResult, Object extractedData, StubSearchProjectionContext context);

	public final StubProjectionNode toRootNode() {
		StubProjectionNode.Builder nodeBuilder = StubProjectionNode.root( typeName() );
		toNode( nodeBuilder );
		return nodeBuilder.build();
	}

	protected abstract String typeName();

	protected abstract void toNode(StubProjectionNode.Builder self);

	protected final void appendInnerNode(StubProjectionNode.Builder self, String innerKey, StubSearchProjection<?> inner) {
		self.inner( innerKey, inner.typeName(), inner::toNode );
	}

	public static <U> StubSearchProjection<U> from(SearchProjection<U> projection) {
		if ( !( projection instanceof StubSearchProjection ) ) {
			throw new AssertionFailure( "Projection " + projection + " must be a StubSearchProjection" );
		}
		return (StubSearchProjection<U>) projection;
	}
}
