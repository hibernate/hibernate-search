package org.hibernate.search.jsr352.test.entity;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Embeddable;

@Embeddable
public class MyDatePK implements Serializable {

	private static final long serialVersionUID = 1L;
	private int year;
	private int month;
	private int day;

	public MyDatePK() {

	}

	public MyDatePK(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;
	}

	public int getYear() {
		return year;
	}

	public int getMonth() {
		return month;
	}

	public int getDay() {
		return day;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public void setDay(int day) {
		this.day = day;
	}

	public Date toDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month - 1); // month is base-0
		calendar.set(Calendar.DAY_OF_MONTH, day);
		return calendar.getTime();
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
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		MyDatePK other = (MyDatePK) obj;
		if ( day != other.day )
			return false;
		if ( month != other.month )
			return false;
		if ( year != other.year )
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MyDatePK [year=" + year + ", month=" + month + ", day=" + day + "]";
	}
}
