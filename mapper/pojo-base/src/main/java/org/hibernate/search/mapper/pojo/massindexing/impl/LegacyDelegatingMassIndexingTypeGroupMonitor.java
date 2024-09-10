/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.OptionalLong;

import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingTypeGroupMonitor;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingTypeGroupMonitorContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingTypeGroupMonitorCreateContext;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public final class LegacyDelegatingMassIndexingTypeGroupMonitor implements MassIndexingTypeGroupMonitor {

	private final MassIndexingMonitor delegate;

	public LegacyDelegatingMassIndexingTypeGroupMonitor(MassIndexingMonitor delegate,
			MassIndexingTypeGroupMonitorCreateContext context) {
		this.delegate = delegate;
	}

	@Override
	public void documentsIndexed(long increment) {
		// do nothing
	}

	@SuppressWarnings("removal")
	@Override
	public void indexingStarted(MassIndexingTypeGroupMonitorContext context) {
		OptionalLong count = context.totalCount();
		if ( count.isPresent() ) {
			delegate.addToTotalCount( count.getAsLong() );
		}
	}

	@Override
	public void indexingCompleted(MassIndexingTypeGroupMonitorContext context) {
		// do nothing
	}
}
