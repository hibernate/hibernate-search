/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

/**
 * Custom key to keep a map of loaded {@code EntityInfo} instances.
 *
 * @author Hardy Ferentschik
 */
public class EntityInfoLoadKey {
	private final Class<?> type;
	private final Object id;

	public EntityInfoLoadKey(Class<?> type, Object id) {
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

		EntityInfoLoadKey entityInfoLoadKey = (EntityInfoLoadKey) o;

		if ( !id.equals( entityInfoLoadKey.id ) ) {
			return false;
		}
		if ( !type.equals( entityInfoLoadKey.type ) ) {
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

	@Override
	public String toString() {
		return "EntityInfoLoadKey{" +
				"type=" + type +
				", id=" + id +
				'}';
	}
}


