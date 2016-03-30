/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Date;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.db.ColumnType;
import org.hibernate.search.db.annotations.IdColumn;
import org.hibernate.search.db.annotations.IdInfo;
import org.hibernate.search.db.annotations.UpdateInfo;

/**
 * @author Martin Braun
 */
@Entity(name = "DateAsStringEntity")
@Table(name = "DateAsStringEntity")
@Indexed
@UpdateInfo(tableName = "DateAsStringEntity", idInfos = {
		@IdInfo(columns = {
				@IdColumn(column = "date", columnType = ColumnType.STRING)
		}, idConverter = DateToStringConverter.class)
}
)
public class DateAsStringEntity {

	@Id
	@Column(name = "date")
	private Date date;

	@Field
	private String someField;

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getSomeField() {
		return someField;
	}

	public void setSomeField(String someField) {
		this.someField = someField;
	}
}
