/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.builtin.time.impl;

import java.time.Year;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;

/**
 * Converts a {@link Year} to a {@link Integer}.
 *
 * @author Davide D'Alto
 */
public class YearBridge implements NumericTimeBridge, TwoWayFieldBridge {

	public static final YearBridge INSTANCE = new YearBridge();

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value != null ) {
			int isoYear = ( (Year) value ).getValue();
			luceneOptions.addNumericFieldToDocument( name, isoYear, document );
		}
	}

	@Override
	public Object get(String name, Document document) {
		Integer isoYear = (Integer) document.getField( name ).numericValue();
		return Year.of( isoYear );
	}

	@Override
	public String objectToString(Object object) {
		if ( object == null ) {
			return null;
		}
		return String.valueOf( ( (Year) object ).getValue() );
	}

	@Override
	public NumericEncodingType getEncodingType() {
		return NumericEncodingType.INTEGER;
	}

}
