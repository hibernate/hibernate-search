/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * Store the date in 3 different fields - year, month, day - to ease Range Query per
 * year, month or day (eg get all the elements of December for the last 5 years).
 *
 * @author Emmanuel Bernard
 */
public class DateSplitBridge implements FieldBridge {

	private static final TimeZone GMT = TimeZone.getTimeZone( "GMT" );

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		Date date = (Date) value;
		Calendar cal = GregorianCalendar.getInstance( GMT );
		cal.setTime( date );
		int year = cal.get( Calendar.YEAR );
		int month = cal.get( Calendar.MONTH ) + 1;
		int day = cal.get( Calendar.DAY_OF_MONTH );

		// set year
		luceneOptions.addFieldToDocument( name + ".year", String.valueOf( year ), document );

		// set month and pad it if needed
		luceneOptions.addFieldToDocument( name + ".month",
				month < 10 ? "0" : "" + String.valueOf( month ), document );

		// set day and pad it if needed
		luceneOptions.addFieldToDocument( name + ".day",
				day < 10 ? "0" : "" + String.valueOf( day ), document );
	}
}
