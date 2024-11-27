/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TopDocsDataCollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldTypeContext;
import org.hibernate.search.backend.lucene.search.highlighter.impl.LuceneAbstractSearchHighlighter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.spi.HighlightProjectionBuilder;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.LeafReaderContext;

public class LuceneFieldHighlightProjection<T> implements LuceneSearchProjection<T> {

	private final Set<String> indexNames;
	private final Analyzer analyzer;
	private final String absoluteFieldPath;
	private final String highlighterName;
	private final String nestedDocumentPath;
	private final LuceneSearchIndexValueFieldTypeContext<?> typeContext;
	private final ProjectionCollector.Provider<String, T> collectorProvider;

	private LuceneFieldHighlightProjection(Builder builder, ProjectionCollector.Provider<String, T> collectorProvider) {
		this( builder.scope, builder.field, builder.highlighterName(), collectorProvider );
	}

	LuceneFieldHighlightProjection(LuceneSearchIndexScope<?> scope,
			LuceneSearchIndexValueFieldContext<?> field,
			String highlighterName, ProjectionCollector.Provider<String, T> collectorProvider) {
		this.indexNames = scope.hibernateSearchIndexNames();
		this.analyzer = field.type().searchAnalyzerOrNormalizer();
		this.absoluteFieldPath = field.absolutePath();
		this.highlighterName = highlighterName;
		this.nestedDocumentPath = field.nestedDocumentPath();
		this.typeContext = field.type();
		this.collectorProvider = collectorProvider;
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
			throw QueryLog.INSTANCE.cannotHighlightInNestedContext(
					context.absoluteCurrentFieldPath(),
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
		context.checkValidField( absoluteFieldPath );
		LuceneAbstractSearchHighlighter highlighter = context.highlighter( highlighterName );
		if ( !typeContext.highlighterTypeSupported( highlighter.type() ) ) {
			throw QueryLog.INSTANCE.highlighterTypeNotSupported( highlighter.type(), absoluteFieldPath );
		}
		highlighter.request( context, absoluteFieldPath );
		if ( !highlighter.isCompatible( collectorProvider ) ) {
			throw QueryLog.INSTANCE.highlighterIncompatibleCardinality();
		}

		return new FieldHighlightExtractor<>( context.absoluteCurrentNestedFieldPath(), highlighter,
				collectorProvider.get()
		);
	}


	private class FieldHighlightExtractor<A> implements Extractor<A, T> {
		private final String parentDocumentPath;
		private final LuceneAbstractSearchHighlighter highlighter;
		private final ProjectionCollector<String, String, A, T> collector;

		private FieldHighlightExtractor(String parentDocumentPath, LuceneAbstractSearchHighlighter highlighter,
				ProjectionCollector<String, String, A, T> collector) {
			this.parentDocumentPath = parentDocumentPath;
			this.highlighter = highlighter;
			this.collector = collector;
		}

		@Override
		public Values<A> values(ProjectionExtractContext context) {
			return highlighter.createValues(
					parentDocumentPath,
					nestedDocumentPath,
					absoluteFieldPath,
					analyzer,
					context,
					collector
			);
		}

		@Override
		public T transform(LoadingResult<?> loadingResult, A extractedData,
				ProjectionTransformContext context) {
			return collector.finish( extractedData );
		}
	}

	public abstract static class HighlighterValues<A, T> extends AbstractNestingAwareAccumulatingValues<String, A> {

		protected LeafReaderContext leafReaderContext;

		protected HighlighterValues(String parentDocumentPath, String nestedDocumentPath,
				TopDocsDataCollectorExecutionContext context,
				ProjectionCollector<String, ?, A, T> collector) {
			super( parentDocumentPath, nestedDocumentPath, collector, context );
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
			return collector.accumulateAll( accumulated, highlight( docId ) );
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
				throw QueryLog.INSTANCE.cannotHighlightFieldFromNestedObjectStructure(
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
		public <V> SearchProjection<V> build(ProjectionCollector.Provider<String, V> collectorProvider) {
			return new LuceneFieldHighlightProjection<>( this, collectorProvider );
		}
	}
}
