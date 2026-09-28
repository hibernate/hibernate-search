package org.hibernate.search.documentation.analysis;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class AdvancedLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		// tag::instance[]
		context.analyzer( "my-standard" ).instance( new StandardAnalyzer() );
		// end::instance[]
	}
}
