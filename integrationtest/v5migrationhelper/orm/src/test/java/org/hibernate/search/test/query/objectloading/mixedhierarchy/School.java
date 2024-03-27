/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.objectloading.mixedhierarchy;

import jakarta.persistence.MappedSuperclass;

/**
 * @author Gunnar Morling
 */
@MappedSuperclass
public class School extends EducationalInstitution {

	School() {
	}

	public School(String name) {
		super( name );
	}
}
