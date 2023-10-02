/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.IndexReaderMetadataResolver;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.LeafReaderContext;

public class LuceneByMappedTypeProjection<P>
		extends AbstractLuceneProjection<P> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<String, LuceneSearchProjection<? extends P>> inners;

	public LuceneByMappedTypeProjection(LuceneSearchIndexScope<?> scope,
			Map<String, LuceneSearchProjection<? extends P>> inners) {
		super( scope );
		this.inners = inners;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "inners=" + inners
				+ "]";
	}

	@Override
	public Extractor<?, P> request(ProjectionRequestContext context) {
		Map<String, Extractor<?, ? extends P>> innerExtractors = new HashMap<>();
		for ( Map.Entry<String, LuceneSearchProjection<? extends P>> entry : inners.entrySet() ) {
			innerExtractors.put( entry.getKey(), entry.getValue().request( context ) );
		}
		return new ByMappedTypeExtractor( innerExtractors );
	}

	private final class ByMappedTypeExtractor implements Extractor<DelegateAndExtractedValue<?, P>, P> {
		private final Map<String, Extractor<?, ? extends P>> inners;

		private ByMappedTypeExtractor(Map<String, Extractor<?, ? extends P>> inners) {
			this.inners = inners;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "["
					+ "inners=" + inners
					+ "]";
		}

		@Override
		public Values<DelegateAndExtractedValue<?, P>> values(ProjectionExtractContext context) {
			Map<String, Values<? extends DelegateAndExtractedValue<?, P>>> innerValues = new HashMap<>();
			for ( Map.Entry<String, Extractor<?, ? extends P>> entry : inners.entrySet() ) {
				innerValues.put( entry.getKey(), new ValuesWrapper<>( entry.getValue(), context ) );
			}
			return new ByMappedTypeValues<>( context.collectorExecutionContext(), innerValues );
		}

		@Override
		public P transform(LoadingResult<?> loadingResult, DelegateAndExtractedValue<?, P> extracted,
				ProjectionTransformContext context) {
			return extracted.transform( loadingResult, context );
		}
	}

	private static final class ValuesWrapper<E, P> implements Values<DelegateAndExtractedValue<E, P>> {
		private final Extractor<E, ? extends P> extractor;
		private final Values<E> values;

		private ValuesWrapper(Extractor<E, ? extends P> extractor, ProjectionExtractContext context) {
			this.extractor = extractor;
			this.values = extractor.values( context );
		}

		@Override
		public void context(LeafReaderContext context) throws IOException {
			values.context( context );
		}

		@Override
		public DelegateAndExtractedValue<E, P> get(int doc) throws IOException {
			return new DelegateAndExtractedValue<>( extractor, values.get( doc ) );
		}
	}

	private static final class ByMappedTypeValues<P> implements Values<DelegateAndExtractedValue<?, P>> {

		private final IndexReaderMetadataResolver metadataResolver;
		private final Map<String, Values<? extends DelegateAndExtractedValue<?, P>>> inners;

		private Values<? extends DelegateAndExtractedValue<?, P>> currentLeafInner;

		public ByMappedTypeValues(CollectorExecutionContext executionContext,
				Map<String, Values<? extends DelegateAndExtractedValue<?, P>>> inners) {
			this.metadataResolver = executionContext.getMetadataResolver();
			this.inners = inners;
		}

		@Override
		public void context(LeafReaderContext context) throws IOException {
			String typeName = metadataResolver.resolveMappedTypeName( context );
			currentLeafInner = inners.get( typeName );
			if ( currentLeafInner == null ) {
				throw log.unexpectedMappedTypeNameForByMappedTypeProjection( typeName, inners.keySet() );
			}
			currentLeafInner.context( context );
		}

		@Override
		public DelegateAndExtractedValue<?, P> get(int doc) throws IOException {
			return currentLeafInner.get( doc );
		}
	}

	private static final class DelegateAndExtractedValue<E, P> {
		private final Extractor<E, ? extends P> delegate;
		private final E extractedValue;

		private DelegateAndExtractedValue(Extractor<E, ? extends P> delegate, E extractedValue) {
			this.delegate = delegate;
			this.extractedValue = extractedValue;
		}

		P transform(LoadingResult<?> loadingResult, ProjectionTransformContext context) {
			return delegate.transform( loadingResult, extractedValue, context );
		}
	}
}
