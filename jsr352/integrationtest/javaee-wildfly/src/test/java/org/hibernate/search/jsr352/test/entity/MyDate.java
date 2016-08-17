package org.hibernate.search.jsr352.test.entity;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
public class MyDate {

	@DocumentId
	@EmbeddedId
	@FieldBridge(impl=MyDatePKBridge.class)
	private MyDatePK myDatePK;

	@Field
	private String weekday;

	public MyDate() {

	}

	public MyDate(int year, int month, int day) {
		this.myDatePK = new MyDatePK( year, month, day );
		this.weekday = MyDate.getWeekday( this.myDatePK.toDate() );
	}

	public MyDatePK getDatePK() {
		return myDatePK;
	}

	public void setDatePK(MyDatePK datePK) {
		this.myDatePK = datePK;
	}

	public String getWeekday () {
		return weekday;
	}

	public void setWeekday(String weekday) {
		this.weekday = weekday;
	}

	public Date toDate() {
		return myDatePK.toDate();
	}

	public static String getWeekday(Date date) {
		return new SimpleDateFormat("EE").format(date).toString();
	}

	@Override
	public String toString() {
		return "MyDate [myDatePK=" + myDatePK + ", weekday=" + weekday + "]";
	}
}
