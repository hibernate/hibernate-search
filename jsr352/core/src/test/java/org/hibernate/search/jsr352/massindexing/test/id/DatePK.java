/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.test.id;

import java.io.Serializable;
import java.util.Locale;

import org.hibernate.search.jsr352.massindexing.test.entity.EntityWithIdClass;

/**
 * Primary key for {@link EntityWithIdClass}.
 *
 * @author Mincong Huang
 */
public class DatePK implements Serializable {

	private static final long serialVersionUID = 1L;

	private int year;

	private int month;

	private int day;

	public DatePK() {

	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getYear() {
		return year;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public int getMonth() {
		return month;
	}

	public void setDay(int day) {
		this.day = day;
	}

	public int getDay() {
		return day;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + day;
		result = prime * result + month;
		result = prime * result + year;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		DatePK that = (DatePK) obj;
		if ( day != that.day ) {
			return false;
		}
		if ( month != that.month ) {
			return false;
		}
		if ( year != that.year ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return String.format( Locale.ROOT, "%04d-%02d-%02d", year, month, day );
	}

}
