/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.depth;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Sanne Grinovero
 */
@Entity
@Table(name = "PERSON_BROKEN_SSN")
@Indexed
public class PersonWithBrokenSocialSecurityNumber {

	private Long id;
	private String name;

	public PersonWithBrokenSocialSecurityNumber() {
	}

	public PersonWithBrokenSocialSecurityNumber(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Field
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@DocumentId
	public String getSSN() {
		// returning a constant to have changes overwrite each other in the test
		return "100";
	}

	public void setSSN(String securityNumber) {
	}

}
