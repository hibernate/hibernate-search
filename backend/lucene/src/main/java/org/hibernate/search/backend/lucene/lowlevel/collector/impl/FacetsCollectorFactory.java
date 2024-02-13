/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
