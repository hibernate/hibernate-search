/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.batchindexing;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * An entity having a property named "id" which is not the identifier.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
@Entity
@Indexed
public class WeirdlyIdentifiedEntity {

	@Id
	@GeneratedValue
	private int testsID;

	private String id;

	public int getTestsID() {
		return this.testsID;
	}

	public void setTestsID(int testsID) {
		this.testsID = testsID;
	}

	@Field
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
