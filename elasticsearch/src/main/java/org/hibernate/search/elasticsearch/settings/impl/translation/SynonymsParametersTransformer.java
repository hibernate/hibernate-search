/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

class SynonymsParametersTransformer implements ParametersTransformer {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final String SOLR_FORMAT = "solr";
	private static final String WORDNET_FORMAT = "wordnet";

	private final Class<?> factoryClass;
	private final ParameterValueTransformer solrSynonymsTransformer;
	private final ParameterValueTransformer wordnetSynonymsTransformer;

	public SynonymsParametersTransformer(Class<?> factoryClass, ResourceLoader resourceLoader) {
		super();
		this.factoryClass = factoryClass;
		this.solrSynonymsTransformer = new SolrSynonymsParameterValueTransformer( resourceLoader );
		this.wordnetSynonymsTransformer = new WordnetSynonymsParameterValueTransformer( resourceLoader );
	}

	@Override
	public Map<String, JsonElement> transform(Map<String, String> luceneParameters) {
		Map<String, JsonElement> result = new LinkedHashMap<>();

		String format = luceneParameters.remove( "format" );
		if ( format != null ) {
			result.put( "format", new JsonPrimitive( format ) );
		}
		else {
			format = SOLR_FORMAT;
		}

		String fileNames = luceneParameters.remove( "synonyms" );
		if ( fileNames != null ) {
			ParameterValueTransformer synonymsTransformer = getSynonymsTransformer( format );
			JsonElement synonyms = synonymsTransformer.transform( fileNames );
			result.put( "synonyms", synonyms );
		}

		return result;
	}

	private ParameterValueTransformer getSynonymsTransformer(String format) {
		switch ( format ) {
			case SOLR_FORMAT:
				return solrSynonymsTransformer;
			case WORDNET_FORMAT:
				return wordnetSynonymsTransformer;
			default:
				throw LOG.unsupportedAnalysisDefinitionParameterValue( factoryClass, "format", format );
		}
	}

}