/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Indexed(index = Hierarchy3_A_B.NAME)
public class Hierarchy3_A_B extends Hierarchy3_A__NonAbstract_NonIndexed {

	public static final String NAME = "H3_A_B";

	public Hierarchy3_A_B(int id) {
		super( id );
	}
}
