/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.OptionalLong;

import org.hibernate.search.mapper.pojo.massindexing.MassIndexingTypeGroupMonitor;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public final class NoOpMassIndexingTypeGroupMonitor implements MassIndexingTypeGroupMonitor {

	public static final NoOpMassIndexingTypeGroupMonitor INSTANCE = new NoOpMassIndexingTypeGroupMonitor();

	private NoOpMassIndexingTypeGroupMonitor() {
	}

	@Override
	public void documentsAdded(long increment) {
		// do nothing
	}

	@Override
	public void indexingStarted(OptionalLong totalCount) {
		// do nothing
	}

	@Override
	public void indexingCompleted() {
		// do nothing
	}
}
