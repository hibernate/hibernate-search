/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.impl.ExtendedBridgeProvider;

/**
 * Creates bridges specific to ES.
 *
 * @author Gunnar Morling
 */
// TODO Handle Calendar
public class ElasticsearchBridgeProvider extends ExtendedBridgeProvider {

	@Override
	public FieldBridge provideFieldBridge(ExtendedBridgeProviderContext bridgeContext) {
		if ( isDate( bridgeContext.getReturnType() ) ) {
			return EsDateBridge.INSTANCE;
		}

		return null;
	}

	private boolean isDate(Class<?> clazz) {
		return "java.util.Date".equals( clazz.getName() );
	}

	// TODO Handle resolution
	private static class EsDateBridge implements TwoWayFieldBridge {

		private static EsDateBridge INSTANCE = new EsDateBridge();

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			if ( value == null ) {
				return;
			}

			luceneOptions.addFieldToDocument( name, convertToString( (Date) value ), document );
		}

		@Override
		public Object get(String name, Document document) {
			Calendar c = DatatypeConverter.parseDateTime( document.get( name ) );
			return c.getTime();
		}

		@Override
		public String objectToString(Object object) {
			return convertToString( (Date) object );
		}

		private String convertToString(Date value) {
			Calendar c = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ENGLISH );
			c.setTime( value );
			return DatatypeConverter.printDateTime( c );
		}
	}
}
