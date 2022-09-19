/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Hierarchy5_A_B__MappedSuperClass extends Hierarchy5_A__Abstract {

	public static final String NAME = "H5_A_B";

	protected Hierarchy5_A_B__MappedSuperClass() {
		// For Hibernate ORM
	}

	public Hierarchy5_A_B__MappedSuperClass(int id) {
		super( id );
	}
}
