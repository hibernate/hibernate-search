/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalysisDefinition;
import org.hibernate.search.exception.AssertionFailure;

import com.google.gson.JsonElement;

class SimpleAnalysisDefinitionFactory<D extends AnalysisDefinition> implements AnalysisDefinitionFactory<D> {

	private final Class<D> targetClass;
	private final String type;
	private final Map<String, String> parameterNameTranslations;
	private final Map<String, ParameterValueTransformer> parameterValueTranslations;
	private final Map<String, JsonElement> staticParameters;

	public SimpleAnalysisDefinitionFactory(Class<D> targetClazz, String type, Map<String, String> parameterNameTranslations,
			Map<String, ParameterValueTransformer> parameterValueTranslations,
			Map<String, JsonElement> staticParameters) {
		this.targetClass = targetClazz;
		this.type = type;
		this.parameterNameTranslations = parameterNameTranslations;
		this.parameterValueTranslations = parameterValueTranslations;
		this.staticParameters = staticParameters;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public D create(Parameter[] parameters) {
		D result;
		try {
			result = targetClass.newInstance();
		}
		catch (InstantiationException | IllegalAccessException e) {
			throw new AssertionFailure( "Unexpected failure while instantiating a definition", e );
		}
		result.setType( type );

		Map<String, JsonElement> parameterMap = new LinkedHashMap<>( staticParameters );

		if ( parameters != null && parameters.length > 0 ) {
			for ( Parameter parameter : parameters ) {
				addParameter( parameterMap, parameter.name(), parameter.value() );
			}
		}

		if ( !parameterMap.isEmpty() ) {
			result.setParameters( parameterMap );
		}

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
				.append( "targetClass = " ).append( targetClass )
				.append( ", type = " ).append( type )
				.append( ", parameterNameTranslations = " ).append( parameterNameTranslations )
				.append( ", parameterValueTranslations = " ).append( parameterValueTranslations )
				.append( ", staticParameters = " ).append( staticParameters )
				.append( "]" )
				.toString();
	}
}
