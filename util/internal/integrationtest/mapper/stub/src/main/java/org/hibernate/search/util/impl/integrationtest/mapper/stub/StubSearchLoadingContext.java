/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;

public class StubSearchLoadingContext implements SearchLoadingContext<DocumentReference> {

	StubSearchLoadingContext() {
	}

	@Override
	public Object unwrap() {
		return this;
	}

	@Override
	public ProjectionHitMapper<DocumentReference> createProjectionHitMapper() {
		return new StubProjectionHitMapper();
	}
}
