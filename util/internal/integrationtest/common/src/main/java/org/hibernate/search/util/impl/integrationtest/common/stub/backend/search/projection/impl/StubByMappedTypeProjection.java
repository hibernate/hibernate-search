/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.test.data.Pair;

final class StubByMappedTypeProjection<P> extends StubSearchProjection<P> {

	private final Map<String, StubSearchProjection<? extends P>> inners;

	StubByMappedTypeProjection(Map<String, StubSearchProjection<? extends P>> inners) {
		this.inners = inners;
	}

	@Override
	public DelegateAndExtractedValue<P> extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		Object selfProjectionFromIndex = projectionFromIndex.next();
		String typeName;
		Object innerProjectionFromIndex;
		if ( selfProjectionFromIndex instanceof Pair ) {
			Pair<?, ?> pair = (Pair<?, ?>) selfProjectionFromIndex;
			typeName = (String) pair.elem0();
			innerProjectionFromIndex = pair.elem1();
		}
		else if ( selfProjectionFromIndex instanceof DocumentReference ) {
			// This is allowed mainly for ease of use, and for backward compatibility
			// with the many pre-existing tests that involve entity loading.
			DocumentReference documentRef = (DocumentReference) selfProjectionFromIndex;
			typeName = documentRef.typeName();
			innerProjectionFromIndex = documentRef;
		}
		else {
			throw new AssertionFailure( "For a by-mapped-type stub projection,"
					+ " the projection value retrieved from the (stubbed) index must be either:"
					+ " a Pair containing the mapped type name, then the value to pass to the corresponding projection for that type,"
					+ " or just a document reference (if the corresponding projection for that type expects only a document reference)." );
		}

		return new DelegateAndExtractedValue<>( inners.get( typeName ),
				projectionHitMapper, Collections.singletonList( innerProjectionFromIndex ).iterator(), context );
	}

	@Override
	@SuppressWarnings("unchecked")
	public P transform(LoadingResult<?> loadingResult, Object extractedData, StubSearchProjectionContext context) {
		return ( (DelegateAndExtractedValue<P>) extractedData ).transform( loadingResult, context );
	}

	@Override
	protected String typeName() {
		return "byMappedType";
	}

	@Override
	protected void toNode(StubProjectionNode.Builder self) {
		for ( Map.Entry<String, StubSearchProjection<? extends P>> entry : inners.entrySet() ) {
			appendInnerNode( self, entry.getKey(), entry.getValue() );
		}
	}

	private static final class DelegateAndExtractedValue<P> {
		private final StubSearchProjection<? extends P> delegate;
		private final Object extractedValue;

		private DelegateAndExtractedValue(StubSearchProjection<? extends P> delegate,
				ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
				StubSearchProjectionContext context) {
			this.delegate = delegate;
			this.extractedValue = delegate.extract( projectionHitMapper, projectionFromIndex, context );
		}

		P transform(LoadingResult<?> loadingResult, StubSearchProjectionContext context) {
			return delegate.transform( loadingResult, extractedValue, context );
		}
	}
}
