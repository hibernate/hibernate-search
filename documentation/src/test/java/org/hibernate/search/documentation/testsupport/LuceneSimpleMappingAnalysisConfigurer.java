/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.testsupport;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

import org.apache.lucene.analysis.compound.DictionaryCompoundWordTokenFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.ngram.EdgeNGramFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;

public class LuceneSimpleMappingAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.analyzer( "english" ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( SnowballPorterFilterFactory.class )
				.param( "language", "English" )
				.tokenFilter( ASCIIFoldingFilterFactory.class );

		context.analyzer( "name" ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( ASCIIFoldingFilterFactory.class );

		context.analyzer( "autocomplete_indexing" ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( SnowballPorterFilterFactory.class )
				.param( "language", "English" )
				.tokenFilter( ASCIIFoldingFilterFactory.class )
				.tokenFilter( EdgeNGramFilterFactory.class )
				.param( "minGramSize", "3" )
				.param( "maxGramSize", "7" );

		// Same as "autocomplete-indexing", but without the edge-ngram filter
		context.analyzer( "autocomplete_query" ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( SnowballPorterFilterFactory.class )
				.param( "language", "English" )
				.tokenFilter( ASCIIFoldingFilterFactory.class );

		// Normalizers

		context.normalizer( "english" ).custom()
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( ASCIIFoldingFilterFactory.class );

		context.normalizer( "name" ).custom()
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( ASCIIFoldingFilterFactory.class );

		context.normalizer( "isbn" ).custom()
				.charFilter( PatternReplaceCharFilterFactory.class )
				.param( "pattern", "-+" )
				.param( "replacement", "" );

		context.normalizer( "lowercase" ).custom()
				.tokenFilter( LowerCaseFilterFactory.class );

		// For AlternativeBinderIT

		context.analyzer( "text_en" ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( SnowballPorterFilterFactory.class )
				.param( "language", "English" )
				.tokenFilter( ASCIIFoldingFilterFactory.class );
		context.analyzer( "text_fr" ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( SnowballPorterFilterFactory.class )
				.param( "language", "French" )
				.tokenFilter( ASCIIFoldingFilterFactory.class );
		context.analyzer( "text_de" ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( DictionaryCompoundWordTokenFilterFactory.class )
				.param( "dictionary", "AlternativeBinderIT/dictionary_de.txt" )
				.tokenFilter( SnowballPorterFilterFactory.class )
				.param( "language", "German" )
				.tokenFilter( ASCIIFoldingFilterFactory.class );
	}
}
