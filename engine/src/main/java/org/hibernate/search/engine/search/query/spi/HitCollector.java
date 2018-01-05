/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

/**
 * A simple hit collector that stores the provided hit for later retrieval,
 * potentially transforming it in the process.
 * <p>
 * Instances are usually provided by {@link HitAggregator#nextCollector()}.
 *
 * @param <T> The type of hit this collector can collect.
 */
public interface HitCollector<T> {

	void collect(T hit);

}
