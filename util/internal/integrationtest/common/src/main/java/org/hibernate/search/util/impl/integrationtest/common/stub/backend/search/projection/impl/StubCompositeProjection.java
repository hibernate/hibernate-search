/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;

class StubCompositeProjection<E, V, A, P> extends StubSearchProjection<P> {

	private final StubSearchProjection<?>[] inners;
	private final ProjectionCompositor<E, V> compositor;
	private final ProjectionCollector<E, V, A, P> collector;
	private final boolean singleValued;

	protected StubCompositeProjection(StubSearchProjection<?>[] inners, ProjectionCompositor<E, V> compositor,
			ProjectionCollector<E, V, A, P> collector, boolean singleValued) {
		this.inners = inners;
		this.compositor = compositor;
		this.collector = collector;
		this.singleValued = singleValued;
	}

	@Override
	public Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		Iterable<?> allInnerProjectionsFromIndex;
		if ( singleValued ) {
			Object singleValue = projectionFromIndex.next();
			allInnerProjectionsFromIndex = singleValue == null ? Collections.emptyList() : Arrays.asList( singleValue );
		}
		else {
			allInnerProjectionsFromIndex = (Iterable<?>) projectionFromIndex.next();
		}

		A accumulated = collector.createInitial();
		for ( Object innerProjectionsFromIndex : allInnerProjectionsFromIndex ) {
			E extractedData = compositor.createInitial();
			Iterator<?> innerProjectionFromIndex = ( (Iterable<?>) innerProjectionsFromIndex ).iterator();
			for ( int i = 0; i < inners.length; i++ ) {
				Object extractedDataForInner = inners[i].extract( projectionHitMapper, innerProjectionFromIndex, context );
				extractedData = compositor.set( extractedData, i, extractedDataForInner );
			}
			accumulated = collector.accumulate( accumulated, extractedData );
		}
		return accumulated;
	}

	@Override
	@SuppressWarnings("unchecked")
	public P transform(LoadingResult<?> loadingResult, Object extractedData, StubSearchProjectionContext context) {
		A accumulated = (A) extractedData;
		for ( int i = 0; i < collector.size( accumulated ); i++ ) {
			E transformedData = collector.get( accumulated, i );
			// Transform in-place
			for ( int j = 0; j < inners.length; j++ ) {
				Object extractedDataForInner = compositor.get( transformedData, j );
				Object transformedDataForInner = inners[j].transform( loadingResult, extractedDataForInner, context );
				transformedData = compositor.set( transformedData, j, transformedDataForInner );
			}
			accumulated = collector.transform( accumulated, i, compositor.finish( transformedData ) );
		}
		return collector.finish( accumulated );
	}

	@Override
	protected String typeName() {
		return "composite";
	}

	@Override
	protected void toNode(StubProjectionNode.Builder self) {
		// Not including the compositor to facilitate assertions
		// self.attribute( "compositor", compositor );
		self.attribute( "collector", collector );
		self.attribute( "singleValued", singleValued );
		for ( StubSearchProjection<?> inner : inners ) {
			appendInnerNode( self, "inner", inner );
		}
	}

	static class Builder implements CompositeProjectionBuilder {

		Builder() {
		}

		@Override
		public final <E, V, P> SearchProjection<P> build(SearchProjection<?>[] inners, ProjectionCompositor<E, V> compositor,
				ProjectionCollector.Provider<V, P> collectorProvider) {
			StubSearchProjection<?>[] typedInners =
					new StubSearchProjection<?>[inners.length];
			for ( int i = 0; i < inners.length; i++ ) {
				typedInners[i] = StubSearchProjection.from( inners[i] );
			}
			return doBuild( typedInners, compositor, collectorProvider.get(), collectorProvider.isSingleValued() );
		}

		protected <E, V, A, P> SearchProjection<P> doBuild(StubSearchProjection<?>[] typedInners,
				ProjectionCompositor<E, V> compositor,
				ProjectionCollector<E, V, A, P> collector, boolean singleValued) {
			return new StubCompositeProjection<>( typedInners, compositor, collector, singleValued );
		}
	}
}
