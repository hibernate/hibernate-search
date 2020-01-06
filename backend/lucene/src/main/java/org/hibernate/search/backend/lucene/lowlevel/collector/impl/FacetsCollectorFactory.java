/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import org.apache.lucene.facet.FacetsCollector;

public class FacetsCollectorFactory implements CollectorFactory<FacetsCollector> {
	public static final CollectorKey<FacetsCollector> KEY = CollectorKey.create();

	public static final CollectorFactory<FacetsCollector> INSTANCE = new FacetsCollectorFactory();

	@Override
	public FacetsCollector createCollector(CollectorExecutionContext context) {
		return new FacetsCollector();
	}

	@Override
	public CollectorKey<FacetsCollector> getCollectorKey() {
		return KEY;
	}
}
