/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.dialect.ElasticsearchTestDialect;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;

public class ElasticsearchTckBackendFeatures extends TckBackendFeatures {

	private ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	@Override
	public boolean geoPointIndexNullAs() {
		return dialect.isGeoPointIndexNullAsPossible();
	}

	@Override
	public boolean worksFineWithStrictAboveRangedQueriesOnDecimalScaledField() {
		return dialect.worksFineWithStrictGraterThanRangedQueriesOnScaledFloatField();
	}
}
