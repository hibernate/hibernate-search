/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;

class SimpleParametersTransformer implements ParametersTransformer {

	private final Map<String, String> parameterNameTranslations;
	private final Map<String, ParameterValueTransformer> parameterValueTranslations;

	public SimpleParametersTransformer(Map<String, String> parameterNameTranslations,
			Map<String, ParameterValueTransformer> parameterValueTranslations) {
		this.parameterNameTranslations = parameterNameTranslations;
		this.parameterValueTranslations = parameterValueTranslations;
	}

	@Override
	public Map<String, JsonElement> transform(Map<String, String> luceneParameters) {
		if ( luceneParameters.isEmpty() ) {
			return Collections.emptyMap();
		}

		Map<String, JsonElement> result = new LinkedHashMap<>();

		for ( Map.Entry<String, String> entry : luceneParameters.entrySet() ) {
			addParameter( result, entry.getKey(), entry.getValue() );
		}

		// We handled all the remaining parameters: remove them as specified in ParametersTransformer
		luceneParameters.clear();

		return result;
	}

	protected void addParameter(Map<String, JsonElement> parameterMap, String luceneParameterName, String value) {
		String elasticsearchParameterName = parameterNameTranslations.get( luceneParameterName );
		if ( elasticsearchParameterName == null ) {
			// Assume the parameter actually exists in Elasticsearch
			elasticsearchParameterName = luceneParameterName;
		}

		ParameterValueTransformer valueTransformer = parameterValueTranslations.get( luceneParameterName );
		if ( valueTransformer == null ) {
			valueTransformer = StringParameterValueTransformer.INSTANCE;
		}

		parameterMap.put( elasticsearchParameterName, valueTransformer.transform( value ) );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "parameterNameTranslations = " ).append( parameterNameTranslations )
				.append( ", parameterValueTranslations = " ).append( parameterValueTranslations )
				.append( "]" )
				.toString();
	}
}
