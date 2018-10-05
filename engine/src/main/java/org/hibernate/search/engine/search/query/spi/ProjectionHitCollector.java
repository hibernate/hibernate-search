/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

/**
 * A hit collector used when projecting: it allows both to retrieve hits as is,
 * and to mark references for later loading.
 * <p>
 * Instances are usually provided by {@link HitAggregator#nextCollector()}.
 */
public interface ProjectionHitCollector extends ReferenceHitCollector, LoadingHitCollector {

	void collectProjection(Object projection);

}
