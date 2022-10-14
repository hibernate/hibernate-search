/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
