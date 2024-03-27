/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.AbstractStubSearchQueryElementFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexNodeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public class StubObjectProjection<E, V, A, P> extends StubCompositeProjection<E, V, A, P> {

	private final String objectFieldPath;

	private StubObjectProjection(String objectFieldPath, StubSearchProjection<?>[] inners,
			ProjectionCompositor<E, V> compositor, ProjectionAccumulator<E, V, A, P> accumulator,
			boolean singleValued) {
		super( inners, compositor, accumulator, singleValued );
		this.objectFieldPath = objectFieldPath;
	}

	@Override
	protected String typeName() {
		return "object";
	}

	@Override
	protected void toNode(StubProjectionNode.Builder self) {
		self.attribute( "objectFieldPath", objectFieldPath );
		super.toNode( self );
	}

	public static class Factory extends AbstractStubSearchQueryElementFactory<CompositeProjectionBuilder> {
		@Override
		public CompositeProjectionBuilder create(StubSearchIndexScope scope,
				StubSearchIndexNodeContext node) {
			return new Builder( node.absolutePath() );
		}
	}

	static class Builder extends StubCompositeProjection.Builder {
		private final String objectFieldPath;

		Builder(String objectFieldPath) {
			this.objectFieldPath = objectFieldPath;
		}

		@Override
		protected <E, V, A, P> SearchProjection<P> doBuild(StubSearchProjection<?>[] typedInners,
				ProjectionCompositor<E, V> compositor, ProjectionAccumulator<E, V, A, P> accumulator,
				boolean singleValued) {
			return new StubObjectProjection<>( objectFieldPath, typedInners, compositor, accumulator, singleValued );
		}
	}
}
