/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsCollectorManager;

public class FacetsCollectorFactory implements CollectorFactory<FacetsCollector, FacetsCollector, FacetsCollectorManager> {
	public static final CollectorKey<FacetsCollector, FacetsCollector> KEY = CollectorKey.create();

	public static final CollectorFactory<FacetsCollector, FacetsCollector, FacetsCollectorManager> INSTANCE =
			new FacetsCollectorFactory();

	@Override
	public FacetsCollectorManager createCollectorManager(CollectorExecutionContext context) {
		return new FacetsCollectorManager();
	}

	@Override
	public CollectorKey<FacetsCollector, FacetsCollector> getCollectorKey() {
		return KEY;
	}
}
