/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.entityindexmapping;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

// tag::indexed-explicitbackend[]
@Entity
@Table(name = "\"user\"")
@Indexed(backend = "backend2")
public class User {
// end::indexed-explicitbackend[]

	@Id
	@GeneratedValue
	private Integer id;

	private String firstName;

	private String lastName;

	public User() {
	}

	public Integer getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
}
