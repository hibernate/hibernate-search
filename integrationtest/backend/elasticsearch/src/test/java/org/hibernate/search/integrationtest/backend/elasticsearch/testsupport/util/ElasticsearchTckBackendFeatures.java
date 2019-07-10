/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.dialect.ElasticsearchTestDialect;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;

class ElasticsearchTckBackendFeatures extends TckBackendFeatures {

	private ElasticsearchTestDialect dialect;

	ElasticsearchTckBackendFeatures(ElasticsearchTestDialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public boolean geoPointIndexNullAs() {
		return dialect.isGeoPointIndexNullAsPossible();
	}

	@Override
	public boolean worksFineWithStrictAboveRangedQueriesOnDecimalScaledField() {
		return dialect.worksFineWithStrictGraterThanRangedQueriesOnScaledFloatField();
	}

	@Override
	public boolean normalizeStringMissingValues() {
		// TODO HSEARCH-3387 Elasticsearch does not apply the normalizer defined on the field
		//   to the String provided as replacement for missing values on sorting
		return false;
	}
}
