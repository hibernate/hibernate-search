/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.search.Collector;

public interface LuceneCollectorFactory<C extends Collector> extends LuceneCollectorKey<C> {

	C createCollector(LuceneCollectorExecutionContext context);

	default boolean applyToNestedDocuments() {
		return false;
	}

	LuceneCollectorFactory<FacetsCollector> FACETS = context -> new FacetsCollector();

}
