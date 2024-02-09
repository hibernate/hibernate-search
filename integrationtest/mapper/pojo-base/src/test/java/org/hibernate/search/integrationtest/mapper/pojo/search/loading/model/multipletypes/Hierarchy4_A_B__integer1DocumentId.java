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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity(name = Hierarchy4_A_B__integer1DocumentId.NAME)
@Indexed
public class Hierarchy4_A_B__integer1DocumentId extends Hierarchy4_A__NonAbstract_NonIndexed {

	public static final String NAME = "H4_A_B";
	public static final PersistenceTypeKey<Hierarchy4_A_B__integer1DocumentId, Integer> PERSISTENCE_KEY =
			new PersistenceTypeKey<>( Hierarchy4_A_B__integer1DocumentId.class, Integer.class );

	@DocumentId
	private int integer1;

	public Hierarchy4_A_B__integer1DocumentId(int id, int integer1) {
		super( id );
		this.integer1 = integer1;
	}
}
