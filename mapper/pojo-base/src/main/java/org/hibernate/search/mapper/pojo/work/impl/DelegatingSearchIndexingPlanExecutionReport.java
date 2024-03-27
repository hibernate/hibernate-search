/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanExecutionReport;

public class DelegatingSearchIndexingPlanExecutionReport implements SearchIndexingPlanExecutionReport {

	private final MultiEntityOperationExecutionReport delegate;

	public DelegatingSearchIndexingPlanExecutionReport(MultiEntityOperationExecutionReport delegate) {
		this.delegate = delegate;
	}

	@Override
	public Optional<Throwable> throwable() {
		return delegate.throwable();
	}

	@Override
	public List<EntityReference> failingEntities() {
		return delegate.failingEntityReferences();
	}

}
