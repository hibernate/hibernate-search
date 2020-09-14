/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.numeric;

import java.math.BigDecimal;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;

public class BigDecimalNumericFieldBridge implements MetadataProvidingFieldBridge, TwoWayFieldBridge {

	private static final BigDecimal storeFactor = BigDecimal.valueOf( 100 );

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value != null ) {
			BigDecimal decimalValue = (BigDecimal) value;
			Long indexedValue = decimalValue.multiply( storeFactor ).longValue();
			luceneOptions.addNumericFieldToDocument( name, indexedValue, document );
		}
	}

	@Override
	public Object get(String name, Document document) {
		String fromLucene = document.get( name );
		BigDecimal storedBigDecimal = new BigDecimal( fromLucene );
		return storedBigDecimal.divide( storeFactor );
	}

	@Override
	public String objectToString(Object object) {
		return object.toString();
	}

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		builder.field( name, FieldType.LONG );
	}
}
