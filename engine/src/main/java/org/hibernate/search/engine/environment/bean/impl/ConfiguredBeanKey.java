/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.impl;

import java.util.Objects;

final class ConfiguredBeanKey<T> {
	private final Class<T> exposedType;
	private final String name;

	ConfiguredBeanKey(Class<T> exposedType, String name) {
		this.exposedType = exposedType;
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}
		ConfiguredBeanKey<?> other = (ConfiguredBeanKey<?>) obj;
		return Objects.equals( exposedType, other.exposedType )
				&& Objects.equals( name, other.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( exposedType, name );
	}
}
