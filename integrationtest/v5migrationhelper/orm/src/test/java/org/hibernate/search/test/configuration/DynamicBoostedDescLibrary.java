/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.configuration;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Test entity using a custom <code>CustomBoostStrategy</code> to set
 * the document boost as the dynScore field.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
@Entity
public class DynamicBoostedDescLibrary {

	@Id
	@GeneratedValue
	private int libraryId;
	private float dynScore;
	private String name;

	public DynamicBoostedDescLibrary() {
		dynScore = 1.0f;
	}


	public int getLibraryId() {
		return libraryId;
	}

	public void setLibraryId(int id) {
		this.libraryId = id;
	}

	public float getDynScore() {
		return dynScore;
	}

	public void setDynScore(float dynScore) {
		this.dynScore = dynScore;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
