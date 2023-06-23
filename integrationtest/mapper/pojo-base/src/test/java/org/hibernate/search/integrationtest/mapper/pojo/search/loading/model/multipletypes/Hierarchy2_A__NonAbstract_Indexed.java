/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Indexed(index = Hierarchy2_A__NonAbstract_Indexed.NAME)
public class Hierarchy2_A__NonAbstract_Indexed {

	public static final String NAME = "H2_A";
	public static final PersistenceTypeKey<Hierarchy2_A__NonAbstract_Indexed, Integer> PERSISTENCE_KEY =
			new PersistenceTypeKey<>( Hierarchy2_A__NonAbstract_Indexed.class, Integer.class );

	@DocumentId
	private Integer id;

	public Hierarchy2_A__NonAbstract_Indexed(int id) {
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
