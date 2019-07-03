/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class DefaultJavaNetURLValueBridge implements ValueBridge<URL, String> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public String toIndexedValue(URL value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.toExternalForm();
	}

	@Override
	public URL fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
		return value == null ? null : toURL( value );
	}

	@Override
	public URL cast(Object value) {
		return (URL) value;
	}

	@Override
	public String parse(String value) {
		if ( value == null ) {
			return null;
		}

		toURL( value );
		return value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	private static URL toURL(String value) {
		try {
			return new URL( value );
		}
		catch (MalformedURLException e) {
			throw log.malformedURL( value, e );
		}
	}
}
