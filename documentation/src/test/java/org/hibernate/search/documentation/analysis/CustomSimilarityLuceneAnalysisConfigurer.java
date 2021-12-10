/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.analysis;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

import org.apache.lucene.search.similarities.ClassicSimilarity;

// tag::include[]
public class CustomSimilarityLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.similarity( new ClassicSimilarity() ); // <1>

		context.analyzer( "english" ).custom() // <2>
				.tokenizer( "standard" )
				.tokenFilter( "lowercase" )
				.tokenFilter( "asciiFolding" );
	}
}
// end::include[]
