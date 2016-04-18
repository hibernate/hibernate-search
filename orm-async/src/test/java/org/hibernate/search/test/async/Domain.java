/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.async;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.db.annotations.IdColumn;
import org.hibernate.search.db.annotations.IdInfo;
import org.hibernate.search.db.annotations.UpdateInfo;
import org.hibernate.search.db.ColumnType;

/**
 * @author Emmanuel Bernard
 */
@Entity(name = "DOMAIN")
@Indexed
@UpdateInfo(tableName = "DOMAIN", idInfos = @IdInfo(columns = @IdColumn(column = "id", columnType = ColumnType.INTEGER), entity = Domain.class))
public class Domain {
	@Id
	@DocumentId
	private Integer id;
	@Field
	private String name;

	public Domain() {
	}

	public Domain(Integer id, String name) {
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
