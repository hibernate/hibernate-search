/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.spi;

/**
 * A hit collector used when projecting: it allows both to retrieve hits as is,
 * and to mark references for later loading.
 * <p>
 * Instances are usually provided by {@link HitAggregator#nextCollector()}.
 *
 * @param <R> The type of references that can be loaded.
 *
 * @see HitCollector
 * @see LoadingHitCollector
 */
public interface ProjectionHitCollector<R> extends HitCollector<Object>, LoadingHitCollector<R> {
}
