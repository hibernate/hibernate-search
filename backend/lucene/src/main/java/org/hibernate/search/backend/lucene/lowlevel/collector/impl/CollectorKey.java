/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TotalHitCountCollector;

/**
 * Tagging interface for collector keys.
 * <p>
 * This is used for de-duplication of collectors, to avoid collecting the same data twice during the same search.
 *
 * @param <C> The type of collector.
 */
public interface CollectorKey<C extends Collector> {

	static <C extends Collector> CollectorKey<C> create() {
		return new CollectorKey<C>() {
		};
	}

	CollectorKey<TotalHitCountCollector> TOTAL_HIT_COUNT = create();

	CollectorKey<TopDocsCollector> TOP_DOCS = create();

	CollectorKey<ChildrenCollector> CHILDREN = create();

}
