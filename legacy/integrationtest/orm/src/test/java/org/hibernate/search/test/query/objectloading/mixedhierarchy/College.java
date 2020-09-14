/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.objectloading.mixedhierarchy;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.search.annotations.Indexed;

/**
 * @author Gunnar Morling
 */
@Entity
@Indexed
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "collegeType", discriminatorType = DiscriminatorType.STRING)
public class College extends EducationalInstitution {

	@Id
	Long identifier;

	College() {
	}

	public College(long identifier, String name) {
		super( name );
		this.identifier = identifier;
	}

	public Long getIdentifier() {
		return identifier;
	}

	public void setIdentifier(long identifier) {
		this.identifier = identifier;
	}
}
