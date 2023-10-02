/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TopDocsDataCollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldTypeContext;
import org.hibernate.search.backend.lucene.search.highlighter.impl.LuceneAbstractSearchHighlighter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.spi.HighlightProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.LeafReaderContext;

public class LuceneFieldHighlightProjection<T> implements LuceneSearchProjection<T> {

	private final Set<String> indexNames;
	private final Analyzer analyzer;
	private final String absoluteFieldPath;
	private final String highlighterName;
	private final String nestedDocumentPath;
	private final LuceneSearchIndexValueFieldTypeContext<?> typeContext;
	private final ProjectionAccumulator.Provider<String, T> accumulatorProvider;

	private LuceneFieldHighlightProjection(Builder builder, ProjectionAccumulator.Provider<String, T> accumulatorProvider) {
		this( builder.scope, builder.field, builder.highlighterName(), accumulatorProvider );
	}

	LuceneFieldHighlightProjection(LuceneSearchIndexScope<?> scope,
			LuceneSearchIndexValueFieldContext<?> field,
			String highlighterName, ProjectionAccumulator.Provider<String, T> accumulatorProvider) {
		this.indexNames = scope.hibernateSearchIndexNames();
		this.analyzer = field.type().searchAnalyzerOrNormalizer();
		this.absoluteFieldPath = field.absolutePath();
		this.highlighterName = highlighterName;
		this.nestedDocumentPath = field.nestedDocumentPath();
		this.typeContext = field.type();
		this.accumulatorProvider = accumulatorProvider;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "absoluteFieldPath=" + absoluteFieldPath
				+ "highlighterName=" + highlighterName
				+ "]";
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	@Override
	public Extractor<?, T> request(ProjectionRequestContext context) {
		if ( context.absoluteCurrentFieldPath() != null ) {
			throw log.cannotHighlightInNestedContext(
					context.absoluteCurrentFieldPath(),
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
		context.checkValidField( absoluteFieldPath );
		LuceneAbstractSearchHighlighter highlighter = context.highlighter( highlighterName );
		if ( !typeContext.highlighterTypeSupported( highlighter.type() ) ) {
			throw log.highlighterTypeNotSupported( highlighter.type(), absoluteFieldPath );
		}
		highlighter.request( context, absoluteFieldPath );
		if ( !highlighter.isCompatible( accumulatorProvider ) ) {
			throw log.highlighterIncompatibleCardinality();
		}

		return new FieldHighlightExtractor<>( context.absoluteCurrentNestedFieldPath(), highlighter,
				accumulatorProvider.get()
		);
	}


	private class FieldHighlightExtractor<A> implements Extractor<A, T> {
		private final String parentDocumentPath;
		private final LuceneAbstractSearchHighlighter highlighter;
		private final ProjectionAccumulator<String, String, A, T> accumulator;

		private FieldHighlightExtractor(String parentDocumentPath, LuceneAbstractSearchHighlighter highlighter,
				ProjectionAccumulator<String, String, A, T> accumulator) {
			this.parentDocumentPath = parentDocumentPath;
			this.highlighter = highlighter;
			this.accumulator = accumulator;
		}

		@Override
		public Values<A> values(ProjectionExtractContext context) {
			return highlighter.createValues(
					parentDocumentPath,
					nestedDocumentPath,
					absoluteFieldPath,
					analyzer,
					context,
					accumulator
			);
		}

		@Override
		public T transform(LoadingResult<?> loadingResult, A extractedData,
				ProjectionTransformContext context) {
			return accumulator.finish( extractedData );
		}
	}

	public abstract static class HighlighterValues<A, T> extends AbstractNestingAwareAccumulatingValues<String, A> {

		protected LeafReaderContext leafReaderContext;

		protected HighlighterValues(String parentDocumentPath, String nestedDocumentPath,
				TopDocsDataCollectorExecutionContext context,
				ProjectionAccumulator<String, ?, A, T> accumulator) {
			super( parentDocumentPath, nestedDocumentPath, accumulator, context );
		}

		@Override
		public void context(LeafReaderContext context) throws IOException {
			super.context( context );
			// store the leaf reader so that we can pass it to highlighter later.
			// using a global index searcher doesn't work as the docs passed to #get() do not match the TopDocs
			// accessible through the ProjectionExtractContext.
			leafReaderContext = context;
		}

		@Override
		protected A accumulate(A accumulated, int docId) throws IOException {
			return accumulator.accumulateAll( accumulated, highlight( docId ) );
		}

		protected abstract List<String> highlight(int doc) throws IOException;
	}

	public static class Factory<F>
			extends AbstractLuceneValueFieldSearchQueryElementFactory<HighlightProjectionBuilder, F> {
		@Override
		public HighlightProjectionBuilder create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			if ( field.nestedDocumentPath() != null ) {
				// see HSEARCH-4841 to remove this limitation.
				throw log.cannotHighlightFieldFromNestedObjectStructure(
						EventContexts.fromIndexFieldAbsolutePath( field.absolutePath() )
				);
			}
			return new Builder( scope, field );
		}
	}

	public static class Builder extends HighlightProjectionBuilder {
		private final LuceneSearchIndexScope<?> scope;
		private final LuceneSearchIndexValueFieldContext<?> field;

		public Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<?> field) {
			super( field.absolutePath() );
			this.scope = scope;
			this.field = field;
		}

		protected String highlighterName() {
			return highlighterName;
		}

		@Override
		public <V> SearchProjection<V> build(ProjectionAccumulator.Provider<String, V> accumulatorProvider) {
			return new LuceneFieldHighlightProjection<>( this, accumulatorProvider );
		}
	}
}
