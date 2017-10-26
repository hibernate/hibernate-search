/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.test.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Mincong Huang
 */
@Entity
@Indexed
public class Person {

	@Id
	@DocumentId
	// We use UTF-8, let's avoid the error "Specified key was too long; max key length is 767 bytes" on MariaDB
	@Column(length = 50)
	private String id;

	@Field
	private String firstName;

	@Field
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
