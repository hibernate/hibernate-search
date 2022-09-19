/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.objectloading.mixedhierarchy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.annotations.Indexed;

/**
 * @author Gunnar Morling
 */
@Indexed
@Entity
public class University extends EducationalInstitution {

	@Id
	Integer id;

	University() {
	}

	public University(int id, String name) {
		super( name );
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
