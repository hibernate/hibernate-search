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
import javax.persistence.IdClass;
import javax.persistence.Table;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.db.ColumnType;
import org.hibernate.search.db.annotations.IdColumn;
import org.hibernate.search.db.annotations.IdInfo;
import org.hibernate.search.db.annotations.UpdateInfo;

/**
 * @author Martin Braun
 */
@Entity(name = "OverrideEntityCustomType")
@Table(name = "OverrideEntityCustomType")
@Indexed
@IdClass(CustomIdClass.class)
@UpdateInfo(tableName = "OverrideEntityCustomType", idInfos = {
		@IdInfo(columns = {
				@IdColumn(column = "ID", columnType = ColumnType.INTEGER),
				@IdColumn(column = "ID2", columnType = ColumnType.INTEGER)
		}, idConverter = CustomIdConverter.class)
}
)
public class OverrideEntityCustomType {

	@Id
	@Column(name = "ID")
	private Integer id;

	@Id
	@Column(name = "ID2")
	private Integer id2;

	@Column(name = "name")
	private String name;

	@DocumentId(name = "id")
	@FieldBridge(impl = CustomIdConverter.class)
	public CustomIdClass getDocumentId() {
		return new CustomIdClass( id, id2 );
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getId2() {
		return id2;
	}

	public void setId2(Integer id2) {
		this.id2 = id2;
	}

	@Field(store = Store.YES)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
