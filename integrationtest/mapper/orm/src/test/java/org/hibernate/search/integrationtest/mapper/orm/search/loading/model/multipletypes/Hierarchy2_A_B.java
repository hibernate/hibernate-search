/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy2_A_B.NAME)
@Indexed(index = Hierarchy2_A_B.NAME)
public class Hierarchy2_A_B extends Hierarchy2_A__NonAbstract_Indexed {

	public static final String NAME = "H2_A_B";

	protected Hierarchy2_A_B() {
		// For Hibernate ORM
	}

	public Hierarchy2_A_B(int id) {
		super( id );
	}
}
