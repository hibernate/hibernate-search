/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.test.entity;

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
	private String id;

	@Field
	private String firstName;

	@Field
	private String familyName;

	public Person() {
	}

	public Person(int id, String firstName, String familyName) {
		this.id = new StringBuilder()
				.append( firstName, 0, 1 )
				.append( familyName, 0, 1 )
				.append( id )
				.toString();
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

	public void setFamilyName(String famillyName) {
		this.familyName = famillyName;
	}

	@Override
	public String toString() {
		return "Person [id=" + id + ", firstName=" + firstName + ", familyName="
				+ familyName + "]";
	}
}
