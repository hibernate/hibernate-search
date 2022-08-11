/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.join.QueryBitSetProducer;
import org.apache.lucene.util.BitSet;

/**
 * A projection that ensures its inner projection is executed on the root, not on an object field.
 *
 * @param <P> The type of the element returned by the projection.
 */
public class LuceneRootContextProjection<P>
		extends AbstractLuceneProjection<P> {

	private final LuceneSearchProjection<P> inner;

	public LuceneRootContextProjection(LuceneSearchIndexScope<?> scope, LuceneSearchProjection<P> inner) {
		super( scope );
		this.inner = inner;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "inner=" + inner
				+ "]";
	}

	@Override
	public Extractor<?, P> request(ProjectionRequestContext context) {
		if ( context.absoluteCurrentFieldPath() == null ) {
			// Already being executed in the root context.
			// Avoid unnecessary overhead and skip the wrapping completely:
			return inner.request( context );
		}
		ProjectionRequestContext innerContext = context.root();
		return new RootContextExtractor<>( context.absoluteCurrentFieldPath(), inner.request( innerContext ) );
	}

	private static class RootContextExtractor<E, P> implements Extractor<E, P> {
		private final String contextAbsoluteFieldPath;
		private final Extractor<E, P> inner;

		private RootContextExtractor(String contextAbsoluteFieldPath,
				Extractor<E, P> inner) {
			this.contextAbsoluteFieldPath = contextAbsoluteFieldPath;
			this.inner = inner;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "["
					+ "contextAbsoluteFieldPath=" + contextAbsoluteFieldPath
					+ "inner=" + inner
					+ "]";
		}

		@Override
		public Values<E> values(ProjectionExtractContext context) {
			return new RootContextValues( inner.values( context ) );
		}

		private class RootContextValues implements Values<E> {
			private final Values<E> inner;
			private final QueryBitSetProducer rootFilter;
			private BitSet rootDocs;

			private RootContextValues(Values<E> inner) {
				this.inner = inner;
				this.rootFilter = new QueryBitSetProducer( Queries.mainDocumentQuery() );
			}


			@Override
			public void context(LeafReaderContext context) throws IOException {
				rootDocs = rootFilter.getBitSet( context );
				inner.context( context );
			}

			@Override
			public E get(int childDoc) throws IOException {
				int rootDoc = rootDocs.nextSetBit( childDoc );
				return inner.get( rootDoc );
			}
		}

		@Override
		public P transform(LoadingResult<?, ?> loadingResult, E extractedData, ProjectionTransformContext context) {
			return inner.transform( loadingResult, extractedData, context );
		}
	}
}
