/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.spi;

import java.util.Map;
import org.hibernate.search.engine.search.common.MultiValue;

public interface TermsAggregationBuilder<K> extends SearchAggregationBuilder<Map<K, Long>> {

	void orderByCountDescending();

	void orderByCountAscending();

	void orderByTermDescending();

	void orderByTermAscending();

	void minDocumentCount(int minDocumentCount);

	void maxTermCount(int maxTermCount);

	void multi(MultiValue multi);
}
