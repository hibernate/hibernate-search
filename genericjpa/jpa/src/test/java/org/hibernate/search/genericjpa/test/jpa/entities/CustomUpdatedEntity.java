/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.genericjpa.annotations.CustomUpdateEntityProvider;
import org.hibernate.search.genericjpa.annotations.Hint;
import org.hibernate.search.genericjpa.annotations.IdColumn;
import org.hibernate.search.genericjpa.annotations.IdInfo;
import org.hibernate.search.genericjpa.annotations.InIndex;
import org.hibernate.search.genericjpa.annotations.UpdateInfo;
import org.hibernate.search.genericjpa.db.ColumnType;

/**
 * Created by Martin on 08.07.2015.
 */
@Entity
@InIndex
@Indexed
@CustomUpdateEntityProvider(impl = CustomUpdatedEntityEntityProvider.class)
@Table(name = "CustomUpdatedEntity")
@UpdateInfo(tableName = "CustomUpdatedEntity", idInfos = @IdInfo(columns =
@IdColumn(column = "id", columnType = ColumnType.LONG), hints = @Hint(key = "testCustomUpdatedEntity", value = "toast")
))
public class CustomUpdatedEntity {

	@Id
	@Column(name = "id")
	private Long id;

	@Column(name = "text")
	@Field
	private String text;

	public Long getId() {
		return id;
	}

	public CustomUpdatedEntity setId(Long id) {
		this.id = id;
		return this;
	}

	public String getText() {
		return text;
	}

	public CustomUpdatedEntity setText(String text) {
		this.text = text;
		return this;
	}
}
