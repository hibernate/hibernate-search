/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.path.validation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

/**
 * @author Davide D'Alto
 */
@Entity
class FieldRenamedEmbeddedEntity {

	@Id
	@GeneratedValue
	Integer id;

	@Field(name = "renamed")
	public String field;

	@OneToOne
	@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "embedded")))
	public FieldRenamedContainerEntity container;
}
