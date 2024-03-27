/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded;

import jakarta.persistence.Embeddable;

import org.hibernate.search.annotations.Field;

/**
 * @author Yoann Rodiere
 */
@Embeddable
public class Resident {
	@Field
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
