/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

@Incubating
public class QueryParameters implements NamedValues {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<String, Object> parameters = new HashMap<>();

	public void add(String parameter, Object value) {
		Contracts.assertNotNullNorEmpty( parameter, "parameter" );
		// TODO: does null parameter values make any sense? seems no, but ...
		parameters.put( parameter, value );
	}

	@Override
	public <T> T get(String parameterName, Class<T> parameterValueType) {
		Object value = parameter( parameterName );
		if ( value == null ) {
			return null;
		}
		if ( parameterValueType.isAssignableFrom( value.getClass() ) ) {
			return parameterValueType.cast( value );
		}
		throw log.unexpectedQueryParameterType( parameterName, parameterValueType, value.getClass() );
	}

	private Object parameter(String parameterName) {
		if ( !parameters.containsKey( parameterName ) ) {
			// TODO: if a value is null ^. see add(..)
			throw log.cannotFindQueryParameter( parameterName );
		}
		return parameters.get( parameterName );
	}

	@Override
	public <T> Optional<T> getOptional(String name, Class<T> paramType) {
		return Optional.ofNullable( get( name, paramType ) );
	}
}
