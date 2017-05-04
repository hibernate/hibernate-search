/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;

class ThrowingUnsupportedParameterValueTransformer implements ParameterValueTransformer {

	private static final Log log = LoggerFactory.make( Log.class );

	private final Class<?> factoryClass;
	private final String parameterName;

	ThrowingUnsupportedParameterValueTransformer(Class<?> factoryClass, String parameterName) {
		this.factoryClass = factoryClass;
		this.parameterName = parameterName;
	}

	@Override
	public JsonElement transform(String parameterValue) {
		throw log.unsupportedAnalysisFactoryParameter( factoryClass, parameterName );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( factoryClass )
				.append( "," )
				.append( parameterName )
				.append( "]" )
				.toString();
	}
}