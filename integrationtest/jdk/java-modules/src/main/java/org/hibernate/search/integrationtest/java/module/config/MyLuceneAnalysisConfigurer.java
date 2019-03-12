/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.java.module.config;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisDefinitionContainerContext;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;

public class MyLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {

	public static final String MY_ANALYZER = "myAnalyzer";

	@Override
	public void configure(LuceneAnalysisDefinitionContainerContext context) {
		context.analyzer( MY_ANALYZER ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.tokenFilter( ASCIIFoldingFilterFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( SnowballPorterFilterFactory.class )
						.param( "language", "English" );
	}
}
