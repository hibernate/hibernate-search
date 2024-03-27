/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.automaticindexing.session.impl;

import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;

@SuppressWarnings("deprecation")
public final class AutomaticIndexingSynchronizationStrategy {
	private AutomaticIndexingSynchronizationStrategy() {
	}

	public static final org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy ASYNC =
			new DelegatingAutomaticIndexingSynchronizationStrategy(
					IndexingPlanSynchronizationStrategy.async() );
	public static final org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy WRITE_SYNC =
			new DelegatingAutomaticIndexingSynchronizationStrategy(
					IndexingPlanSynchronizationStrategy.writeSync() );
	public static final org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy READ_SYNC =
			new DelegatingAutomaticIndexingSynchronizationStrategy(
					IndexingPlanSynchronizationStrategy.readSync() );
	public static final org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy SYNC =
			new DelegatingAutomaticIndexingSynchronizationStrategy(
					IndexingPlanSynchronizationStrategy.sync() );
}
