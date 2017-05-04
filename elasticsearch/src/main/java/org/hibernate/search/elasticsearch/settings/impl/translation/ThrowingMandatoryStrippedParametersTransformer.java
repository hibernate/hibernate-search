/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import java.util.Collections;
import java.util.Map;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;

class ThrowingMandatoryStrippedParametersTransformer implements ParametersTransformer {

	private static final Log log = LoggerFactory.make( Log.class );

	private final Class<?> factoryClass;
	private final String parameterName;
	private final String expectedValue;

	ThrowingMandatoryStrippedParametersTransformer(Class<?> factoryClass, String parameterName, String expectedValue) {
		this.factoryClass = factoryClass;
		this.parameterName = parameterName;
		this.expectedValue = expectedValue;
	}

	@Override
	public Map<String, JsonElement> transform(Map<String, String> luceneParameters) {
		String actualValue = luceneParameters.remove( parameterName );

		if ( !expectedValue.equals( actualValue ) ) {
			throw log.invalidAnalysisFactoryParameter( factoryClass, parameterName, expectedValue, actualValue );
		}

		return Collections.emptyMap();
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( factoryClass )
				.append( "," )
				.append( parameterName )
				.append( "," )
				.append( expectedValue )
				.append( "]" )
				.toString();
	}
}