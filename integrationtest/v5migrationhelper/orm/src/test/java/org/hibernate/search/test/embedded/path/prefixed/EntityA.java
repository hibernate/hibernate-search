/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.path.prefixed;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * @author Davide D'Alto
 */
@Entity
@Indexed
class EntityA {

	@Id
	@GeneratedValue
	public int id;

	@OneToOne
	@IndexedEmbedded(depth = 0, prefix = "prefixed_", includePaths = { "idx_field" })
	public EntityB b;

	public EntityA() {
	}

	public EntityA(EntityB b) {
		this.b = b;
		this.b.a = this;
	}

}
