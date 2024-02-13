/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.util.Collection;

import org.apache.lucene.search.Collector;

/**
 * Tagging interface for collector keys.
 * <p>
 * This is used for de-duplication of collectors, to avoid collecting the same data twice during the same search.
 *
 * @param <C> The type of collector.
 * @param <T> The type of the results produced after merging multiple collectors via {@link org.apache.lucene.search.CollectorManager#reduce(Collection)}
 */
public interface CollectorKey<C extends Collector, T> {

	static <C extends Collector, T> CollectorKey<C, T> create() {
		return new CollectorKey<C, T>() {
		};
	}

}
