/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.multipletypes;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity(name = Hierarchy8_A_D_Cacheable.NAME)
@Indexed(index = Hierarchy8_A_D_Cacheable.NAME)
@Cacheable
// Does NOT implement Interface2, on contrary to B and C
public class Hierarchy8_A_D_Cacheable extends Hierarchy8_A__Abstract {

	public static final String NAME = "H8_A_D";

	protected Hierarchy8_A_D_Cacheable() {
		// For Hibernate ORM
	}

	public Hierarchy8_A_D_Cacheable(int id) {
		super( id );
	}
}
