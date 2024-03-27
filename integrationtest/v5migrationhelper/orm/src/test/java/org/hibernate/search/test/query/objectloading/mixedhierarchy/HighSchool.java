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
public class HighSchool extends School {

	@Id
	Long identifier;

	HighSchool() {
	}

	public HighSchool(long identifier, String name) {
		super( name );
		this.identifier = identifier;
	}

	public Long getIdentifier() {
		return identifier;
	}

	public void setIdentifier(long identifier) {
		this.identifier = identifier;
	}
}
