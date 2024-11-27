/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;

public interface CollectorFactory<C extends Collector, T, CM extends CollectorManager<C, T>> {

	CM createCollectorManager(CollectorExecutionContext context) throws IOException;

	CollectorKey<C, T> getCollectorKey();

}
