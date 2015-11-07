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
import javax.persistence.IdClass;
import javax.persistence.Table;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.genericjpa.annotations.IdColumn;
import org.hibernate.search.genericjpa.annotations.IdInfo;
import org.hibernate.search.genericjpa.annotations.InIndex;
import org.hibernate.search.genericjpa.annotations.UpdateInfo;
import org.hibernate.search.genericjpa.db.ColumnType;

@Entity
@Table(name = "MultipleColumnsIdEntity")
@InIndex
@Indexed
@IdClass(ID.class)
@UpdateInfo(tableName = "MultipleColumnsIdEntity", idInfos = @IdInfo(columns = {
		@IdColumn(column = "firstId", columnType = ColumnType.CUSTOM, columnDefinition = "VARCHAR(100)"),
		@IdColumn(column = "secondId", columnType = ColumnType.STRING)
}, idConverter = MultipleColumnsIdEntityIdConverter.class))
public class MultipleColumnsIdEntity {

	@Id
	@Column(name = "firstId", columnDefinition = "VARCHAR(100)")
	private String firstId;
	@Id
	@Column(name = "secondId", columnDefinition = "VARCHAR(100)")
	private String secondId;

	@Column(name = "info")
	private String info;

	@DocumentId
	@FieldBridge(impl = IDFieldBridge.class)
	public ID getDocumentId() {
		ID ret = new ID();
		ret.setFirstId( this.getFirstId() );
		ret.setSecondId( this.getSecondId() );
		return ret;
	}

	@Field
	public String getInfo() {
		return this.info;
	}

	public String getFirstId() {
		return firstId;
	}

	public void setFirstId(String firstId) {
		this.firstId = firstId;
	}

	public String getSecondId() {
		return secondId;
	}

	public void setSecondId(String secondId) {
		this.secondId = secondId;
	}

	public void setInfo(String info) {
		this.info = info;
	}
}
