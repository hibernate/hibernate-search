/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Indexed(index = Hierarchy5_A_B_D.NAME)
public class Hierarchy5_A_B_D extends Hierarchy5_A_B__MappedSuperClass {

	public static final String NAME = "H5_A_B_D";

	public Hierarchy5_A_B_D(int id) {
		super( id );
	}
}
