/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
