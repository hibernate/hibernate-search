/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalysisDefinition;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

class JsonPassThroughAnalysisDefinitionFactory<D extends AnalysisDefinition> implements AnalysisDefinitionFactory<D> {

	static final Log LOG = LoggerFactory.make( Log.class );

	private final Class<D> targetClass;

	private final Class<?> factoryClass;

	private final JsonParser jsonParser = new JsonParser();

	public JsonPassThroughAnalysisDefinitionFactory(Class<D> targetClass, Class<?> factoryClass) {
		super();
		this.targetClass = targetClass;
		this.factoryClass = factoryClass;
	}

	@Override
	public String getType() {
		throw new UnsupportedOperationException( "This factory defines the definition type based on the Hibernate Search parameters." );
	}

	@Override
	public D create(Map<String,String> parameters) {
		D result;
		try {
			result = targetClass.newInstance();
		}
		catch (InstantiationException | IllegalAccessException e) {
			throw new AssertionFailure( "Unexpected failure while instanciating a definition", e );
		}

		Map<String, JsonElement> parameterMap = new LinkedHashMap<>();

		for ( Entry<String, String> parameter : parameters.entrySet() ) {
			String name = parameter.getKey();
			String value = parameter.getValue();
			switch ( name ) {
			case "type":
				result.setType( parseJsonString( name, value ) );
				break;
			default:
				parameterMap.put( name, parseJson( name, value ) );
				break;
			}
		}

		if ( !parameterMap.isEmpty() ) {
			result.setParameters( parameterMap );
		}

		return result;
	}

	private String parseJsonString(String name, String value) {
		try {
			/*
			 * Use getAsJsonPrimitive() first in order to throw an exception
			 * if the element is an array, number, or other
			 */
			return jsonParser.parse( value ).getAsJsonPrimitive().getAsString();
		}
		catch (JsonParseException | ClassCastException | IllegalStateException e) {
			throw LOG.invalidAnalysisDefinitionJsonStringParameter( factoryClass, name, e.getLocalizedMessage(), e );
		}
	}

	private JsonElement parseJson(String name, String value) {
		try {
			return jsonParser.parse( value );
		}
		catch (JsonParseException e) {
			throw LOG.invalidAnalysisDefinitionJsonParameter( factoryClass, name, e.getLocalizedMessage(), e );
		}
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "targetClass = " ).append( targetClass )
				.append( "]" )
				.toString();
	}

}