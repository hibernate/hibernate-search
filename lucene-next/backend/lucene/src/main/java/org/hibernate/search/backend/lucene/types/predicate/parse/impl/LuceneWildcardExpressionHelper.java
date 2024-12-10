/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.predicate.parse.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;

/**
 * Copied and adapted from {@code org.apache.lucene.queryparser.classic.QueryParserBase#analyzeWildcard}
 * of <a href="https://github.com/apache/lucene-solr">Apache Lucene and Solr</a>.
 * <p>
 * Allows to normalize a wildcard expression term.
 */
public class LuceneWildcardExpressionHelper {

	private static final Pattern WILDCARD_PATTERN = Pattern.compile( "(\\\\.)|([?*]+)" );

	private LuceneWildcardExpressionHelper() {
	}

	public static BytesRef analyzeWildcard(Analyzer analyzer, String field, String termStr) {
		if ( analyzer == AnalyzerConstants.KEYWORD_ANALYZER ) {
			// Optimization when analysis is disabled
			return new BytesRef( termStr );
		}

		// best effort to not pass the wildcard characters and escaped characters through #normalize
		Matcher wildcardMatcher = WILDCARD_PATTERN.matcher( termStr );
		BytesRefBuilder sb = new BytesRefBuilder();
		int last = 0;

		while ( wildcardMatcher.find() ) {
			if ( wildcardMatcher.start() > 0 ) {
				String chunk = termStr.substring( last, wildcardMatcher.start() );
				BytesRef normalized = analyzer.normalize( field, chunk );
				sb.append( normalized );
			}
			//append the matched group - without normalizing
			sb.append( new BytesRef( wildcardMatcher.group() ) );

			last = wildcardMatcher.end();
		}
		if ( last < termStr.length() ) {
			String chunk = termStr.substring( last );
			BytesRef normalized = analyzer.normalize( field, chunk );
			sb.append( normalized );
		}
		return sb.toBytesRef();
	}
}
