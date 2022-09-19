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
@Entity
@Indexed
public class PrimarySchool extends School {

	@Id
	Short id;

	PrimarySchool() {
	}

	public PrimarySchool(short id, String name) {
		super( name );
		this.id = id;
	}

	public Short getId() {
		return id;
	}

	public void setId(Short id) {
		this.id = id;
	}
}
