/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.Objects;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.QueryBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldQueryFactory;
import org.hibernate.search.backend.lucene.document.model.impl.MatchQueryOptions;
import org.hibernate.search.backend.lucene.document.model.impl.RangeQueryOptions;
import org.hibernate.search.backend.lucene.types.formatter.impl.StringFieldFormatter;
import org.hibernate.search.backend.lucene.util.impl.AnalyzerUtils;

public class StringFieldQueryFactory implements LuceneFieldQueryFactory {

	private final Analyzer analyzerOrNormalizer;

	private final boolean tokenized;

	private final QueryBuilder queryBuilder;

	public StringFieldQueryFactory(Analyzer analyzerOrNormalizer, boolean tokenized, QueryBuilder queryBuilder) {
		this.analyzerOrNormalizer = analyzerOrNormalizer;
		this.tokenized = tokenized;
		this.queryBuilder = queryBuilder;
	}

	@Override
	public Query createMatchQuery(String fieldName, Object value, MatchQueryOptions matchQueryOptions) {
		String stringValue = (String) value;

		if ( queryBuilder != null ) {
			return queryBuilder.createBooleanQuery( fieldName, stringValue, matchQueryOptions.getOperator() );
		}
		else {
			// we are in the case where we a have a normalizer here as the analyzer case has already been treated by
			// the queryBuilder case above

			return new TermQuery( new Term( fieldName, getAnalyzedValue( analyzerOrNormalizer, fieldName, stringValue ) ) );
		}
	}

	@Override
	public Query createRangeQuery(String fieldName, Object lowerLimit, Object upperLimit, RangeQueryOptions rangeQueryOptions) {
		// Note that a range query only makes sense if only one token is returned by the analyzer
		// and we should even consider forcing having a normalizer here, instead of supporting
		// range queries on analyzed fields.

		return TermRangeQuery.newStringRange(
				fieldName,
				getAnalyzedValue( analyzerOrNormalizer, fieldName, (String) lowerLimit ),
				getAnalyzedValue( analyzerOrNormalizer, fieldName, (String) upperLimit ),
				// we force the true value if the limit is null because of some Lucene checks down the hill
				lowerLimit == null ? true : !rangeQueryOptions.isExcludeLowerLimit(),
				upperLimit == null ? true : !rangeQueryOptions.isExcludeUpperLimit()
		);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( StringFieldFormatter.class != obj.getClass() ) {
			return false;
		}

		StringFieldQueryFactory other = (StringFieldQueryFactory) obj;

		return Objects.equals( analyzerOrNormalizer, other.analyzerOrNormalizer ) &&
				tokenized == other.tokenized &&
				Objects.equals( queryBuilder, other.queryBuilder );
	}

	@Override
	public int hashCode() {
		return Objects.hash( analyzerOrNormalizer, tokenized, queryBuilder );
	}

	private static String getAnalyzedValue(Analyzer analyzerOrNormalizer, String fieldName, String stringValue) {
		if ( analyzerOrNormalizer == null ) {
			return stringValue;
		}

		return AnalyzerUtils.analyzeSortableValue( analyzerOrNormalizer, fieldName, stringValue );
	}
}
