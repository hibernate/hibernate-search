/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class BackendsInfo {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// Using a linked hash map to preserve the order
	private Map<Optional<String>, BackendInfo> backendsByNames = new LinkedHashMap<>();

	public Collection<BackendInfo> values() {
		return backendsByNames.values();
	}

	public void collect(Optional<String> name, boolean multiTenancyEnabled) {
		backendsByNames.merge( name, new BackendInfo( name, multiTenancyEnabled ),
				(info1, info2) -> {
					if ( info1.multiTenancyEnabled == info2.multiTenancyEnabled ) {
						return info1;
					}
					if ( name.isPresent() ) {
						throw log.differentMultiTenancyNamedBackend( name.get() );
					}
					throw log.differentMultiTenancyDefaultBackend();
				}
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
	}
}
