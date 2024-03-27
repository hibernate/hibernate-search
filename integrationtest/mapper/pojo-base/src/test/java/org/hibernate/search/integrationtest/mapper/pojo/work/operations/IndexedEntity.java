/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

@Indexed(index = IndexedEntity.INDEX)
public final class IndexedEntity {

	public static final String INDEX = "IndexedEntity";

	public static IndexedEntity of(int id) {
		IndexedEntity entity = new IndexedEntity();
		entity.id = id;
		entity.value = String.valueOf( id );
		return entity;
	}

	@DocumentId
	Integer id;

	@GenericField
	String value;

	@IndexedEmbedded
	@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "containing")))
	ContainedEntity contained;
}
