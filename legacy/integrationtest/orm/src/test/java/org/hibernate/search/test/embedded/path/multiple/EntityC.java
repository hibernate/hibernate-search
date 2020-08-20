/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.embedded.path.multiple;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;

/**
 * @author Davide D'Alto
 */
@Entity
class EntityC {

	@Id
	@GeneratedValue
	public int id;

	@OneToOne(mappedBy = "indexed")
	@ContainedIn
	public EntityB b;

	@OneToOne(mappedBy = "skipped")
	@ContainedIn
	public EntityB b2;

	@Fields({ @Field(name = "renamedSkipped"), @Field(name = "renamed") })
	public String field;

	@Field
	public String anotherField;

	@Field
	public String skipped = "skipped";

	public EntityC() {
	}

	public EntityC(String indexed) {
		this.field = indexed;
	}

}
