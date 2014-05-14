/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.test.bridge.DateSplitBridge;
import org.hibernate.search.test.bridge.PaddedIntegerBridge;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
@Entity
@Indexed(index = "Day")
public class CalendarDay {

	private Integer id;
	private Date day;

	@Id
	@DocumentId
	@FieldBridge(impl = PaddedIntegerBridge.class) // test needs to sort on the id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Field(analyze = Analyze.NO, store = Store.YES)
	@FieldBridge(impl = DateSplitBridge.class)
	public Date getDay() {
		return day;
	}

	public void setDay(Date day) {
		this.day = day;
	}

	@Transient
	public CalendarDay setDayFromItalianString(String day) throws ParseException {
		DateFormat formatter = DateFormat.getDateInstance( DateFormat.SHORT, Locale.ITALY );
		this.day = formatter.parse( day );
		return this;
	}

}
