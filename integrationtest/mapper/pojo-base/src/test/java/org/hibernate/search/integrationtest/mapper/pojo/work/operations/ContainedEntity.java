/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
