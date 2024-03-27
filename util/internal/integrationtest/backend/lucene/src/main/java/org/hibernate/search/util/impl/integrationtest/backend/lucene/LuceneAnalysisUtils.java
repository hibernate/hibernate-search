/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.backend.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public final class LuceneAnalysisUtils {
	private LuceneAnalysisUtils() {
	}

	public static List<String> analyze(Analyzer analyzer, String absoluteFieldPath,
			String inputString)
			throws IOException {
		final List<String> tokenList = new ArrayList<>();
		try ( TokenStream stream = analyzer.tokenStream( absoluteFieldPath, inputString ) ) {
			CharTermAttribute term = stream.addAttribute( CharTermAttribute.class );
			stream.reset();
			while ( stream.incrementToken() ) {
				String s = new String( term.buffer(), 0, term.length() );
				tokenList.add( s );
			}
			stream.end();
		}
		return tokenList;
	}
}
