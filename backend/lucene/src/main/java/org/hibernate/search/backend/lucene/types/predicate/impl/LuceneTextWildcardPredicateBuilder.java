/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSearchNestedPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.types.predicate.parse.impl.LuceneWildcardExpressionHelper;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;

class LuceneTextWildcardPredicateBuilder extends AbstractLuceneSearchNestedPredicateBuilder
		implements WildcardPredicateBuilder<LuceneSearchPredicateBuilder> {

	protected final String absoluteFieldPath;

	private final Analyzer analyzerOrNormalizer;

	private String pattern;

	LuceneTextWildcardPredicateBuilder(String absoluteFieldPath, List<String> nestedPathHierarchy, Analyzer analyzerOrNormalizer) {
		super( nestedPathHierarchy );
		this.absoluteFieldPath = absoluteFieldPath;
		this.analyzerOrNormalizer = analyzerOrNormalizer;
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
