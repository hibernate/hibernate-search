/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSingleFieldPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.types.predicate.parse.impl.LuceneWildcardExpressionHelper;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;

class LuceneTextWildcardPredicateBuilder extends AbstractLuceneSingleFieldPredicateBuilder
		implements WildcardPredicateBuilder<LuceneSearchPredicateBuilder> {

	private final Analyzer analyzerOrNormalizer;

	private String pattern;

	LuceneTextWildcardPredicateBuilder(LuceneSearchFieldContext<?> field) {
		super( field );
		this.analyzerOrNormalizer = field.type().searchAnalyzerOrNormalizer();
	}

	@Override
	public void pattern(String wildcardPattern) {
		this.pattern = wildcardPattern;
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		BytesRef analyzedWildcard = LuceneWildcardExpressionHelper.analyzeWildcard( analyzerOrNormalizer, absoluteFieldPath, pattern );
		return new WildcardQuery( new Term( absoluteFieldPath, analyzedWildcard ) );
	}
}
