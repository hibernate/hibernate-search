/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class BackendsInfo {

	// Using a linked hash map to preserve the order
	private Map<Optional<String>, BackendInfo> backendsByNames = new LinkedHashMap<>();

	public Collection<BackendInfo> values() {
		return backendsByNames.values();
	}

	public void collect(Optional<String> name, boolean multiTenancyEnabled) {
		backendsByNames.merge( name, new BackendInfo( name, multiTenancyEnabled ),
				// multiTenancyEnabled = info1.multiTenancyEnabled || info2.multiTenancyEnabled
				(info1, info2) -> ( info1.multiTenancyEnabled ) ? info1 : info2
		);
	}

	public static final class BackendInfo {
		// {@code Optional.empty()} means "the default backend"
		private final Optional<String> name;
		private final boolean multiTenancyEnabled;

		public BackendInfo(Optional<String> name, boolean multiTenancyEnabled) {
			this.name = name;
			this.multiTenancyEnabled = multiTenancyEnabled;
		}

		public Optional<String> name() {
			return name;
		}

		public boolean multiTenancyEnabled() {
			return multiTenancyEnabled;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			BackendInfo that = (BackendInfo) o;
			return multiTenancyEnabled == that.multiTenancyEnabled && Objects.equals( name, that.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name, multiTenancyEnabled );
		}
	}
}
