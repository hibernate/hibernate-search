/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Indexed(index = Hierarchy1_A_B.NAME)
public class Hierarchy1_A_B extends Hierarchy1_A__Abstract {

	public static final String NAME = "H1_A_B";

	public Hierarchy1_A_B(int id) {
		super( id );
	}
}
