/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = EntityIdDocumentIdIndexedEntity.NAME)
@Indexed(index = EntityIdDocumentIdIndexedEntity.NAME)
public class EntityIdDocumentIdIndexedEntity {

	public static final String NAME = "EntityIdDocumentId";

	@Id
	private Integer id;

	protected EntityIdDocumentIdIndexedEntity() {
	}

	public EntityIdDocumentIdIndexedEntity(int id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}
}
