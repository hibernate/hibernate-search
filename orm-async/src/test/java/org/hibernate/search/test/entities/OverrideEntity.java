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

import org.hibernate.search.annotations.Field;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.db.ColumnType;
import org.hibernate.search.db.annotations.IdColumn;
import org.hibernate.search.db.annotations.IdInfo;
import org.hibernate.search.db.annotations.UpdateInfo;

/**
 * @author Emmanuel Bernard
 * @author Martin Braun
 */
@Entity(name = "ENTITY")
@Table(name = "ENTITY")
@UpdateInfo(tableName = "ENTITY", updateTableName = "ENTITY_UPDATES",
		idInfos = @IdInfo(columns =
		@IdColumn(column = "ID", updateTableColumn = "UPDATE_TABLE_ID", columnType = ColumnType.INTEGER)))
@Indexed
public class OverrideEntity {

	@Id
	@DocumentId
	@Column(name = "ID")
	private Integer id;

	@Field
	@Column
	private String name;

	public OverrideEntity() {
	}

	public OverrideEntity(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}