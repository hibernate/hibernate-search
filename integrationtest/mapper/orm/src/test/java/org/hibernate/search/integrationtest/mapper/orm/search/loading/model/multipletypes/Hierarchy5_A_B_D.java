/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import javax.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy5_A_B_D.NAME)
@Indexed(index = Hierarchy5_A_B_D.NAME)
public class Hierarchy5_A_B_D extends Hierarchy5_A_B__MappedSuperClass {

	public static final String NAME = "H5_A_B_D";

	protected Hierarchy5_A_B_D() {
		// For Hibernate ORM
	}

	public Hierarchy5_A_B_D(int id) {
		super( id );
	}
}
