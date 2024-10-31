/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.logging.impl.BackendLog;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;

public final class BackendsInfo {

	// Using a linked hash map to preserve the order
	private final Map<Optional<String>, BackendInfo> backendsByNames = new LinkedHashMap<>();

	public Collection<BackendInfo> values() {
		return backendsByNames.values();
	}

	public void collect(Optional<String> name, TenancyMode tenancyMode) {
		backendsByNames.merge( name, new BackendInfo( name, tenancyMode ),
				(info1, info2) -> {
					if ( info1.tenancyMode == info2.tenancyMode ) {
						return info1;
					}
					if ( name.isPresent() ) {
						throw BackendLog.INSTANCE.differentMultiTenancyNamedBackend( name.get() );
					}
					throw BackendLog.INSTANCE.differentMultiTenancyDefaultBackend();
				}
		);
	}

	public static final class BackendInfo {
		// {@code Optional.empty()} means "the default backend"
		private final Optional<String> name;
		private final TenancyMode tenancyMode;

		public BackendInfo(Optional<String> name, TenancyMode tenancyMode) {
			this.name = name;
			this.tenancyMode = tenancyMode;
		}

		public Optional<String> name() {
			return name;
		}

		public TenancyMode tenancyStrategy() {
			return tenancyMode;
		}
	}
}
