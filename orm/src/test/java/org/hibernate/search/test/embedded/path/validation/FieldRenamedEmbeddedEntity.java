/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.path.validation;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;

/**
 * @author Davide D'Alto
 */
@Entity
class FieldRenamedEmbeddedEntity {

	@Id
	@GeneratedValue
	Integer id;

	@Field(name = "renamed")
	public String field;

	@OneToOne
	@ContainedIn
	public FieldRenamedContainerEntity container;
}
