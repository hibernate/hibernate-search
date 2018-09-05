/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.elasticsearch.settings.impl.model.AnalysisDefinition;
import org.hibernate.search.util.impl.ClassLoaderHelper;

import com.google.gson.JsonElement;

class SimpleAnalysisDefinitionFactory<D extends AnalysisDefinition> implements AnalysisDefinitionFactory<D> {

	private final Class<D> targetClass;
	private final String type;
	private final List<ParametersTransformer> parameterTransformers;

	public SimpleAnalysisDefinitionFactory(Class<D> targetClazz, String type, List<ParametersTransformer> parameterTransformers) {
		this.targetClass = targetClazz;
		this.type = type;
		this.parameterTransformers = parameterTransformers;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public D create(Map<String,String> parameters) {
		D result = ClassLoaderHelper.untypedInstanceFromClass( targetClass, "analysis definition" );
		result.setType( type );

		Map<String, JsonElement> elasticsearchParameterMap = new LinkedHashMap<>();
		for ( ParametersTransformer transformer : parameterTransformers ) {
			elasticsearchParameterMap.putAll( transformer.transform( parameters ) );
		}

		if ( !elasticsearchParameterMap.isEmpty() ) {
			result.setParameters( elasticsearchParameterMap );
		}

		return result;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "targetClass = " ).append( targetClass )
				.append( ", type = " ).append( type )
				.append( ", parameterTransformers = " ).append( parameterTransformers )
				.append( "]" )
				.toString();
	}
}
