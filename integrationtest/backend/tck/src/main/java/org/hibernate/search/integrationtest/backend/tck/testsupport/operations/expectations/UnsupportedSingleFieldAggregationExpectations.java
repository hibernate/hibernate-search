/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;

public interface UnsupportedSingleFieldAggregationExpectations {

	String aggregationName();

	void trySetup(SearchAggregationFactory<DocumentReference> factory, String fieldPath);

}
