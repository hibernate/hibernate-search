/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.path.simple;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

/**
 * @author Davide D'Alto
 */
@Entity
class EntityB {

	@Id
	@GeneratedValue
	public int id;

	@OneToOne
	@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "b")))
	public EntityA a;

	@OneToOne
	@IndexedEmbedded
	public EntityC indexed;

	@OneToOne
	@IndexedEmbedded
	public EntityC skipped;

	public EntityB() {
	}

	public EntityB(EntityC indexed, EntityC skipped) {
		this.indexed = indexed;
		indexed.b = this;

		if ( skipped != null ) {
			this.skipped = skipped;
			skipped.b = this;
		}
	}

}
