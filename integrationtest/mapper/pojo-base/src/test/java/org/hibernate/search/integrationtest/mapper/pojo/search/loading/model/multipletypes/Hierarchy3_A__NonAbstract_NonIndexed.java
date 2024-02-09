/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.multipletypes;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubEntityLoadingBinder;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity(name = Hierarchy3_A__NonAbstract_NonIndexed.NAME,
		loadingBinder = @EntityLoadingBinderRef(type = StubEntityLoadingBinder.class))
public class Hierarchy3_A__NonAbstract_NonIndexed {

	public static final String NAME = "H3_A";
	public static final PersistenceTypeKey<Hierarchy3_A__NonAbstract_NonIndexed, Integer> PERSISTENCE_KEY =
			new PersistenceTypeKey<>( Hierarchy3_A__NonAbstract_NonIndexed.class, Integer.class );

	@DocumentId
	private Integer id;

	public Hierarchy3_A__NonAbstract_NonIndexed(int id) {
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
