/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisDefinitionContainerContext;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

class LuceneSimpleMappingAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisDefinitionContainerContext context) {
		context.analyzer( "english" ).instance( new StandardAnalyzer() );
		context.analyzer( "name" ).instance( new StandardAnalyzer() );
		context.normalizer( "english" ).custom()
				.tokenFilter( ASCIIFoldingFilterFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class );
	}
}
