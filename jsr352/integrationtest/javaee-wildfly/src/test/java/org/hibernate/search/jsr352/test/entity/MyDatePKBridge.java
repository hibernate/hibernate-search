package org.hibernate.search.jsr352.test.entity;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

public class MyDatePKBridge implements TwoWayFieldBridge {

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		MyDatePK pk = (MyDatePK) value;

		// cast int to string
		String year = String.format( "%04d", pk.getYear() );
		String month = String.format( "%02d", pk.getMonth() );
		String day = String.format( "%02d", pk.getDay() );

		// store each property in a unique field
		luceneOptions.addFieldToDocument( name + ".year", year,	document );
		luceneOptions.addFieldToDocument( name + ".month", month, document );
		luceneOptions.addFieldToDocument( name + ".day", day, document );

		// store the unique string representation in the named field
		luceneOptions.addFieldToDocument( name, objectToString( pk ), document );
	}

	@Override
	public Object get(String name, Document document) {
		MyDatePK pk = new MyDatePK();

		IndexableField field = document.getField( name + ".year" );
		pk.setYear( Integer.valueOf( field.stringValue() ) );
		field = document.getField( name + ".month" );
		pk.setMonth( Integer.valueOf( field.stringValue() ) );
		field = document.getField( name + ".day" );
		pk.setDay( Integer.valueOf( field.stringValue() ) );

		return pk;
	}

	@Override
	public String objectToString(Object object) {
		MyDatePK pk = (MyDatePK) object;
		// cast int to string
		String year = String.format( "%04d", pk.getYear() );
		String month = String.format( "%02d", pk.getMonth() );
		String day = String.format( "%02d", pk.getDay() );
		return String.format( "%s%s%s", year, month, day );
	}
}
