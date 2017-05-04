/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import java.util.Map;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;

class MapParameterValueTransformer implements ParameterValueTransformer {

	private static final Log log = LoggerFactory.make( Log.class );

	private final Class<?> factoryClass;
	private final String parameterName;
	private final Map<String, JsonElement> translations;

	public MapParameterValueTransformer(Class<?> factoryClass, String parameterName, Map<String, JsonElement> translations) {
		this.factoryClass = factoryClass;
		this.parameterName = parameterName;
		this.translations = translations;
	}

	@Override
	public JsonElement transform(String parameterValue) {
		JsonElement translatedValue = translations.get( parameterValue );
		if ( translatedValue == null ) {
			throw log.unsupportedAnalysisDefinitionParameterValue( factoryClass, parameterName, parameterValue );
		}
		return translatedValue;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( factoryClass )
				.append( "," )
				.append( parameterName )
				.append( "," )
				.append( translations )
				.append( "]" )
				.toString();
	}
}