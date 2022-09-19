/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.mapping;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = IndexedEntity.INDEX)
@Indexed(index = IndexedEntity.INDEX)
public class IndexedEntity {
	static final String INDEX = "IndexedEntity";

	@Id
	private Integer id;

	@Basic
	@GenericField
	private String indexedField;

	public IndexedEntity() {
	}

	public IndexedEntity(Integer id, String indexedField) {
		this.id = id;
		this.indexedField = indexedField;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getIndexedField() {
		return indexedField;
	}

	public void setIndexedField(String indexedField) {
		this.indexedField = indexedField;
	}
}
