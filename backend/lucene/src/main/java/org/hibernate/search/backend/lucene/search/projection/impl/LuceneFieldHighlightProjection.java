/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.highlighter.impl.LuceneSearchHighlighter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.projection.dsl.spi.HighlightProjectionBuilder;

import org.apache.lucene.analysis.Analyzer;

public class LuceneFieldHighlightProjection implements LuceneSearchProjection<List<String>> {

	private final Set<String> indexNames;
	private final Analyzer analyzer;
	private final String absoluteFieldPath;
	private final String highlighterName;

	private LuceneFieldHighlightProjection(Builder builder) {
		this( builder.scope, builder.field, builder.highlighterName() );
	}

	LuceneFieldHighlightProjection(LuceneSearchIndexScope<?> scope,
			LuceneSearchIndexValueFieldContext<?> field,
			String highlighterName) {
		this.indexNames = scope.hibernateSearchIndexNames();
		this.analyzer = field.type().searchAnalyzerOrNormalizer();
		this.absoluteFieldPath = field.absolutePath();
		this.highlighterName = highlighterName;
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
	public FieldHighlightExtractor request(ProjectionRequestContext context) {
		context.requireAllStoredFields();
		return new FieldHighlightExtractor( context.highlighter( highlighterName ) );
	}


	private class FieldHighlightExtractor implements Extractor<List<String>, List<String>> {
		private final LuceneSearchHighlighter highlighter;

		private FieldHighlightExtractor(LuceneSearchHighlighter highlighter) {
			this.highlighter = highlighter;
		}

		@Override
		public Values<List<String>> values(ProjectionExtractContext context) {
			return highlighter.createValues( absoluteFieldPath, analyzer, context );
		}

		@Override
		public List<String> transform(LoadingResult<?, ?> loadingResult, List<String> extractedData,
				ProjectionTransformContext context) {
			return extractedData;
		}
	}

	public static class Factory<F>
			extends AbstractLuceneValueFieldSearchQueryElementFactory<HighlightProjectionBuilder, F> {
		@Override
		public HighlightProjectionBuilder create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
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
		public LuceneFieldHighlightProjection build() {
			return new LuceneFieldHighlightProjection( this );
		}
	}
}
