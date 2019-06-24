/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class DefaultJavaNetURIValueBridge implements ValueBridge<URI, String> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
	public StandardIndexFieldTypeOptionsStep<?, String> bind(ValueBridgeBindingContext<URI> context) {
		return context.getTypeFactory().asString()
				.projectionConverter( PojoDefaultURIFromDocumentFieldValueConverter.INSTANCE );
	}

	@Override
	public String toIndexedValue(URI value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.toString();
	}

	@Override
	public URI cast(Object value) {
		return (URI) value;
	}

	@Override
	public String parse(String value) {
		if ( value == null ) {
			return null;
		}

		PojoDefaultURIFromDocumentFieldValueConverter.toURI( value );
		return value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	private static class PojoDefaultURIFromDocumentFieldValueConverter
			implements FromDocumentFieldValueConverter<String, URI> {
		private static final PojoDefaultURIFromDocumentFieldValueConverter INSTANCE = new PojoDefaultURIFromDocumentFieldValueConverter();

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( URI.class );
		}

		@Override
		public URI convert(String value, FromDocumentFieldValueConvertContext context) {
			return value == null ? null : toURI( value );
		}

		@Override
		public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
			return INSTANCE.equals( other );
		}

		private static URI toURI(String value) {
			try {
				return new URI( value );
			}
			catch (URISyntaxException e) {
				throw log.badURISyntax( value, e );
			}
		}
	}
}
