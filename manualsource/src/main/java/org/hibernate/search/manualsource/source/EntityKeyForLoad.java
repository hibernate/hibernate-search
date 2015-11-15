/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.source;

import java.io.Serializable;

/**
 * Identify an entity instance by its type and id.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class EntityKeyForLoad {
	private final Class<?> type;
	private final Serializable id;

	public Class<?> getType() {
		return type;
	}

	public Serializable getId() {
		return id;
	}

	public EntityKeyForLoad(Class<?> type, Serializable id) {
		this.type = type;
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		EntityKeyForLoad that = (EntityKeyForLoad) o;

		if ( !id.equals( that.id ) ) {
			return false;
		}
		if ( !type.equals( that.type ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + id.hashCode();
		return result;
	}
}
