/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy7_A_D.NAME)
@Indexed(index = Hierarchy7_A_D.NAME)
// Does NOT implement Interface1, on contrary to B and C
public class Hierarchy7_A_D extends Hierarchy7_A__Abstract {

	public static final String NAME = "H7_A_D";

	protected Hierarchy7_A_D() {
		// For Hibernate ORM
	}

	public Hierarchy7_A_D(int id) {
		super( id );
	}
}
