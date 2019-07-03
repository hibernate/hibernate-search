/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = Hierarchy1_A__Abstract.NAME)
public abstract class Hierarchy1_A__Abstract {

	public static final String NAME = "H1_A";

	@Id
	private Integer id;

	protected Hierarchy1_A__Abstract() {
		// For Hibernate ORM
	}

	public Hierarchy1_A__Abstract(int id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + id + "]";
	}
}
