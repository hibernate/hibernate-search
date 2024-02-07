/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading;

import java.util.Objects;

public class PersistenceTypeKey<E, I> {
	public final Class<E> entityType;
	private final Class<I> idType;

	public PersistenceTypeKey(Class<E> entityType, Class<I> idType) {
		this.entityType = entityType;
		this.idType = idType;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		PersistenceTypeKey<?, ?> typesKey = (PersistenceTypeKey<?, ?>) o;
		return idType.equals( typesKey.idType ) && entityType.equals( typesKey.entityType );
	}

	@Override
	public int hashCode() {
		return Objects.hash( idType, entityType );
	}
}
