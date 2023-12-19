/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.spi;

import org.hibernate.search.engine.search.predicate.spi.KnnPredicateBuilder;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface ElasticsearchKnnPredicateBuilder extends KnnPredicateBuilder {
	void numberOfCandidates(int numberOfCandidates);
}
