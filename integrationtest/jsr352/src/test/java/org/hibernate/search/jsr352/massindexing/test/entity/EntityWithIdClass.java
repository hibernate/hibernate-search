/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.test.entity;

import java.io.Serializable;
import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.jsr352.massindexing.test.id.DatePK;

/**
 * Entity containing multiple {@link Id} attributes.
 *
 * @author Mincong Huang
 */
@Entity
@Indexed
@IdClass( DatePK.class )
public class EntityWithIdClass implements Serializable {

	private static final long serialVersionUID = 1L;

	private int year;

	private int month;

	private int day;

	private int documentId;

	public EntityWithIdClass() {
	}

	public EntityWithIdClass(LocalDate d) {
		year = d.getYear();
		month = d.getMonthValue();
		day = d.getDayOfMonth();
		documentId = year * 1_00_00 + month * 1_00 + day;
	}

	public void setYear(int year) {
		this.year = year;
	}

	@Id
	public int getYear() {
		return year;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	@Id
	public int getMonth() {
		return month;
	}

	public void setDay(int day) {
		this.day = day;
	}

	@Id
	public int getDay() {
		return day;
	}

	@DocumentId
	public int getDocumentId() {
		return documentId;
	}

	public void setDocumentId(int documentId) {
		this.documentId = documentId;
	}

}
