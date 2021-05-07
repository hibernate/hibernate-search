/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

public final class ContainedEntity {

	public static ContainedEntity of(int id) {
		ContainedEntity entity = new ContainedEntity();
		entity.id = id;
		entity.value = "contained" + id;
		entity.containing = IndexedEntity.of( id );
		entity.containing.contained = entity;
		return entity;
	}

	@DocumentId
	Integer id;

	@GenericField
	String value;

	IndexedEntity containing;
}
