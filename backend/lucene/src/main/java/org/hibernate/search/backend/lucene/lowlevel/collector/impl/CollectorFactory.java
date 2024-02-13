/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;

public interface CollectorFactory<C extends Collector, T, CM extends CollectorManager<C, T>> {

	CM createCollectorManager(CollectorExecutionContext context) throws IOException;

	CollectorKey<C, T> getCollectorKey();

}
