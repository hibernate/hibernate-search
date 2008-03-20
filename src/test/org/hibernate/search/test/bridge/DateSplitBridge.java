//$Id$
package org.hibernate.search.test.bridge;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.bridge.FieldBridge;

/**
 * Store the date in 3 different field year, month, day
 * to ease Range Query per year, month or day
 * (eg get all the elements of december for the last 5 years)
 *
 * @author Emmanuel Bernard
 */
public class DateSplitBridge implements FieldBridge {
	private final static TimeZone GMT = TimeZone.getTimeZone( "GMT" );

	public void set(String name, Object value, Document document, Field.Store store, Field.Index index, Field.TermVector termVector, Float boost) {
		Date date = (Date) value;
		Calendar cal = GregorianCalendar.getInstance( GMT );
		cal.setTime( date );
		int year = cal.get( Calendar.YEAR );
		int month = cal.get( Calendar.MONTH ) + 1;
		int day = cal.get( Calendar.DAY_OF_MONTH );
		//set year
		Field field = new Field( name + ".year", String.valueOf( year ), store, index, termVector );
		if ( boost != null ) field.setBoost( boost );
		document.add( field );
		//set month and pad it if needed
		field = new Field( name + ".month", month < 10 ? "0" : "" + String.valueOf( month ), store, index, termVector );
		if ( boost != null ) field.setBoost( boost );
		document.add( field );
		//set day and pad it if needed
		field = new Field( name + ".day", day < 10 ? "0" : "" + String.valueOf( day ), store, index, termVector );
		if ( boost != null ) field.setBoost( boost );
		document.add( field );
	}
}
