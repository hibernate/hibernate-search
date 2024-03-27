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
@Indexed
@Entity
public class University extends EducationalInstitution {

	@Id
	Integer id;

	University() {
	}

	public University(int id, String name) {
		super( name );
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
