/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

public final class DefaultJavaNetURLValueBridge implements ValueBridge<URL, URI> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	@SuppressWarnings("unchecked") // The bridge resolver performs the checks using reflection
	public StandardIndexFieldTypeContext<?, URI> bind(ValueBridgeBindingContext<URL> context) {
		return context.getTypeFactory().asUri()
				.projectionConverter( PojoDefaultURLFromDocumentFieldValueConverter.INSTANCE );
	}

	@Override
	public URI toIndexedValue(URL value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : toURI( value );
	}

	@Override
	public URL cast(Object value) {
		return (URL) value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		return true;
	}

	private static class PojoDefaultURLFromDocumentFieldValueConverter
			implements FromDocumentFieldValueConverter<URI, URL> {
		private static final PojoDefaultURLFromDocumentFieldValueConverter INSTANCE = new PojoDefaultURLFromDocumentFieldValueConverter();

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( URL.class );
		}

		@Override
		public URL convert(URI value, FromDocumentFieldValueConvertContext context) {
			return value == null ? null : toURL( value );
		}

		@Override
		public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
			return INSTANCE.equals( other );
		}
	}

	private static URL toURL(URI value) {
		URL url = null;
		try {
			url = value.toURL();
		}
		catch (MalformedURLException e) {
			log.malformedURL( value, e );
		}

		return url;
	}

	private static URI toURI(URL value) {
		URI uri = null;
		try {
			uri = value.toURI();
		}
		catch (URISyntaxException e) {
			log.badURISyntax( value, e );
		}

		return uri;
	}
}
