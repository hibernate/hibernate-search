/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.impl;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public final class LuceneDefaultAnalysisConfigurer implements LuceneAnalysisConfigurer {
	public static final LuceneDefaultAnalysisConfigurer INSTANCE = new LuceneDefaultAnalysisConfigurer();

	private final Analyzer standard = new StandardAnalyzer();

	private LuceneDefaultAnalysisConfigurer() {
	}

	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.analyzer( AnalyzerNames.DEFAULT ).instance( standard );
	}
}
