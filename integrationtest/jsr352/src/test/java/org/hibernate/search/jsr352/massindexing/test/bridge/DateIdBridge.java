/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.test.bridge;

import java.util.Locale;

import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.jsr352.massindexing.test.id.EmbeddableDateId;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

/**
 * @author Mincong Huang
 */
public class DateIdBridge implements TwoWayFieldBridge, MetadataProvidingFieldBridge {

	@Override
	public void set(String name, Object myDateIdObj, Document document, LuceneOptions luceneOptions) {
		EmbeddableDateId myDateId = (EmbeddableDateId) myDateIdObj;

		// cast int to string
		String year = String.format( Locale.ROOT, "%04d", myDateId.getYear() );
		String month = String.format( Locale.ROOT, "%02d", myDateId.getMonth() );
		String day = String.format( Locale.ROOT, "%02d", myDateId.getDay() );

		// store each property in a unique field
		luceneOptions.addFieldToDocument( name + "_year", year, document );
		luceneOptions.addFieldToDocument( name + "_month", month, document );
		luceneOptions.addFieldToDocument( name + "_day", day, document );

		// store the unique string representation in the named field
		luceneOptions.addFieldToDocument( name, objectToString( myDateId ), document );
	}

	@Override
	public Object get(String name, Document document) {
		EmbeddableDateId myDateId = new EmbeddableDateId();
		IndexableField idxField;

		idxField = document.getField( name + "_year" );
		myDateId.setYear( Integer.valueOf( idxField.stringValue() ) );

		idxField = document.getField( name + "_month" );
		myDateId.setMonth( Integer.valueOf( idxField.stringValue() ) );

		idxField = document.getField( name + "_day" );
		myDateId.setDay( Integer.valueOf( idxField.stringValue() ) );

		return myDateId;
	}

	@Override
	public String objectToString(Object myDateIdObj) {
		return String.valueOf( myDateIdObj );
	}

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		builder.field( name, FieldType.STRING )
				.field( name + "_year", FieldType.STRING )
				.field( name + "_month", FieldType.STRING )
				.field( name + "_day", FieldType.STRING );
	}
}
