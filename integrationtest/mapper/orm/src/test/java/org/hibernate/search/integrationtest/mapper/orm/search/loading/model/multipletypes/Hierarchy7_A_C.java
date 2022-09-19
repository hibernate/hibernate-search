/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy7_A_C.NAME)
@Indexed(index = Hierarchy7_A_C.NAME)
public class Hierarchy7_A_C extends Hierarchy7_A__Abstract implements Interface1 {

	public static final String NAME = "H7_A_C";

	protected Hierarchy7_A_C() {
		// For Hibernate ORM
	}

	public Hierarchy7_A_C(int id) {
		super( id );
	}
}
