/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;

/**
 * A loading hit collector that expects references in place of hits,
 * and will make sure the given reference is later loaded from an external data source
 * (potentially in a batch).
 * <p>
 * Instances are usually provided by {@link HitAggregator#nextCollector()},
 * loading is usually performed by an {@link ObjectLoader}.
 */
public interface LoadingHitCollector {

	void collectForLoading(DocumentReference reference);

}
