/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.objectloading.mixedhierarchy;

import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;

import org.hibernate.search.annotations.Indexed;

/**
 * @author Gunnar Morling
 */
@MappedSuperclass
@Indexed
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class School extends EducationalInstitution {

	School() {
	}

	public School(String name) {
		super( name );
	}
}
