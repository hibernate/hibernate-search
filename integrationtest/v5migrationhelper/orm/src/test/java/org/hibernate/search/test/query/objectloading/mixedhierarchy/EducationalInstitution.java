/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.objectloading.mixedhierarchy;

import jakarta.persistence.MappedSuperclass;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;

/**
 * @author Gunnar Morling
 */
@MappedSuperclass
public class EducationalInstitution {

	@Field(analyze = Analyze.NO)
	private String name;

	public EducationalInstitution() {
	}

	public EducationalInstitution(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
