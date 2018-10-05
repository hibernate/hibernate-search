/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import org.hibernate.search.engine.search.DocumentReference;

/**
 * A hit collector that expects a document reference.
 * <p>
 * The reference will be returned as part of the results,
 * potentially after having been transformed.
 * <p>
 * Instances are usually provided by {@link HitAggregator#nextCollector()}.
 */
public interface ReferenceHitCollector {

	void collectReference(DocumentReference reference);

}
