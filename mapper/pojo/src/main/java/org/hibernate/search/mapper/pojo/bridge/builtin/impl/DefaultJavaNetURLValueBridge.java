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

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
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
	@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
	public StandardIndexFieldTypeOptionsStep<?, String> bind(ValueBridgeBindingContext<URL> context) {
		return context.getTypeFactory().asString()
				.projectionConverter( PojoDefaultURLFromDocumentFieldValueConverter.INSTANCE );
	}

	@Override
	public String toIndexedValue(URL value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.toExternalForm();
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

		PojoDefaultURLFromDocumentFieldValueConverter.toURL( value );
		return value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	private static class PojoDefaultURLFromDocumentFieldValueConverter
			implements FromDocumentFieldValueConverter<String, URL> {
		private static final PojoDefaultURLFromDocumentFieldValueConverter INSTANCE = new PojoDefaultURLFromDocumentFieldValueConverter();

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( URL.class );
		}

		@Override
		public URL convert(String value, FromDocumentFieldValueConvertContext context) {
			return value == null ? null : toURL( value );
		}

		@Override
		public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
			return INSTANCE.equals( other );
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
}
