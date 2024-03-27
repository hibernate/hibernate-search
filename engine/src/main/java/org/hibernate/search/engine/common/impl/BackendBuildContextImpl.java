/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.impl;

import java.util.Optional;

import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;

class BackendBuildContextImpl extends DelegatingBuildContext implements BackendBuildContext {

	private final TenancyMode tenancyMode;

	private final Optional<String> backendNameOptional;

	BackendBuildContextImpl(RootBuildContext delegate, TenancyMode tenancyMode, Optional<String> backendNameOptional) {
		super( delegate );
		this.tenancyMode = tenancyMode;
		this.backendNameOptional = backendNameOptional;
	}

	@Override
	public boolean multiTenancyEnabled() {
		return TenancyMode.MULTI_TENANCY.equals( tenancyMode );
	}

	@Override
	public Optional<String> backendName() {
		return backendNameOptional;
	}
}
