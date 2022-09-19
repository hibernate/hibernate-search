/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.objectloading.mixedhierarchy;

import jakarta.persistence.MappedSuperclass;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;

/**
 * @author Gunnar Morling
 */
@MappedSuperclass
public class EducationalInstitution {

	@Field(analyze = Analyze.NO)
	private String name;

	public EducationalInstitution() {
	}

	public EducationalInstitution(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
