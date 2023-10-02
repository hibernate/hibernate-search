/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.java.modules.pojo.standalone.lucene.config;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

public class MyLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {

	public static final String MY_ANALYZER = "myAnalyzer";

	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.analyzer( MY_ANALYZER ).custom()
				.tokenizer( "standard" )
				.tokenFilter( "lowercase" )
				.tokenFilter( "asciiFolding" );
	}
}
