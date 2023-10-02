/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

/**
 * @author Mincong Huang
 */
@Entity
@Indexed
public class Person {

	@Id
	private String id;

	@FullTextField
	private String firstName;

	@FullTextField
	private String familyName;

	public Person() {
	}

	public Person(String id, String firstName, String familyName) {
		this.id = id;
		this.firstName = firstName;
		this.familyName = familyName;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getFamilyName() {
		return familyName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	@Override
	public String toString() {
		return "Person [id=" + id + ", firstName=" + firstName + ", familyName=" + familyName
				+ "]";
	}
}
