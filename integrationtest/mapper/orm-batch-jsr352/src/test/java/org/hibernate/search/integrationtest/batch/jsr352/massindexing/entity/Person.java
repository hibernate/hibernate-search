/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity;

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
	private String famillyName;

	public Person() {
	}

	public Person(String id, String firstName, String famillyName) {
		this.id = id;
		this.firstName = firstName;
		this.famillyName = famillyName;
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

	public String getFamillyName() {
		return famillyName;
	}

	public void setFamillyName(String famillyName) {
		this.famillyName = famillyName;
	}

	@Override
	public String toString() {
		return "Person [id=" + id + ", firstName=" + firstName + ", famillyName=" + famillyName
				+ "]";
	}
}
