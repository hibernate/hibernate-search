/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy4_A_D.NAME)
@Indexed(index = Hierarchy4_A_D.NAME)
public class Hierarchy4_A_D extends Hierarchy4_A__NonAbstract_NonIndexed {

	public static final String NAME = "H4_A_D";

	protected Hierarchy4_A_D() {
		// For Hibernate ORM
	}

	public Hierarchy4_A_D(int id) {
		super( id );
	}
}
