/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;

class ElasticsearchTckBackendFeatures extends TckBackendFeatures {

	private ElasticsearchTestDialect dialect;

	ElasticsearchTckBackendFeatures(ElasticsearchTestDialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public boolean geoPointIndexNullAs() {
		return dialect.supportsGeoPointIndexNullAs();
	}

	@Override
	public boolean worksFineWithStrictAboveRangedQueriesOnDecimalScaledField() {
		return dialect.supportsStrictGreaterThanRangedQueriesOnScaledFloatField();
	}

	@Override
	public boolean normalizeStringMissingValues() {
		// TODO HSEARCH-3387 Elasticsearch does not apply the normalizer defined on the field
		//   to the String provided as replacement for missing values on sorting
		return false;
	}

	@Override
	public boolean supportsManyRoutingKeys() {
		// TODO HSEARCH-3655 AWS signing fails when using multiple routing keys
		return ! ElasticsearchTestHostConnectionConfiguration.get().isAwsSigningEnabled();
	}

	@Override
	public boolean zonedDateTimeDocValueHasUTCZoneId() {
		return dialect.zonedDateTimeDocValueHasUTCZoneId();
	}

	@Override
	public boolean nonCanonicalRangeInAggregations() {
		// Elasticsearch only supports [a, b), (-Infinity, b), [a, +Infinity), but not [a, b] for example.
		return false;
	}

	@Override
	public boolean lenientOnMultiIndexesCompatibilityChecks() {
		return true;
	}

	@Override
	public boolean fastTimeoutResolution() {
		// TODO HSEARCH-3785 Elasticsearch timeout resolution is not very fast
		return false;
	}
}
