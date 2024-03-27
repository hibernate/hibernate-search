/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.objectloading.mixedhierarchy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Indexed;

/**
 * @author Gunnar Morling
 */
@Entity
@Indexed
public class PrimarySchool extends School {

	@Id
	Short id;

	PrimarySchool() {
	}

	public PrimarySchool(short id, String name) {
		super( name );
		this.id = id;
	}

	public Short getId() {
		return id;
	}

	public void setId(Short id) {
		this.id = id;
	}
}
