/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.configuration;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisDefinitionContainerContext;
import org.hibernate.search.integrationtest.backend.tck.configuration.DefaultAnalysisDefinitions;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class DefaultITAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisDefinitionContainerContext context) {
		context.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD.name ).instance( new StandardAnalyzer() );
		context.normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ).custom()
				.tokenFilter( LowerCaseFilterFactory.class );
	}
}
