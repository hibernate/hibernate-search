/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = NonEntityIdDocumentIdIndexedEntity.NAME)
@Indexed(index = NonEntityIdDocumentIdIndexedEntity.NAME)
public class NonEntityIdDocumentIdIndexedEntity {

	public static final String NAME = "NonEntityIdDocumentId";

	@Id
	private Integer id;

	@DocumentId
	private Integer documentId;

	protected NonEntityIdDocumentIdIndexedEntity() {
	}

	public NonEntityIdDocumentIdIndexedEntity(int id, int documentId) {
		this.id = id;
		this.documentId = documentId;
	}

	public Integer getId() {
		return id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}
}
