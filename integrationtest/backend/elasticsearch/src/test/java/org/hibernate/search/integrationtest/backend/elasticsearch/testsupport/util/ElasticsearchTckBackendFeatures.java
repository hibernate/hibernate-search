/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;

class ElasticsearchTckBackendFeatures extends TckBackendFeatures {

	private final ElasticsearchTestDialect dialect;

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
	public boolean normalizesStringMissingValues() {
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

	@Override
	public boolean sortByFieldValue(TestedFieldStructure fieldStructure, Class<?> fieldType, SortMode sortMode) {
		if (
				fieldStructure.isInNested()
				&& sortMode == SortMode.MAX
				&& ( Float.class.equals( fieldType )
						|| Double.class.equals( fieldType )
						|| BigInteger.class.equals( fieldType )
						|| BigDecimal.class.equals( fieldType ) )
		) {
			// For some reason, ES 5.6 seems to evaluate the max to 0 when a nested document
			// has a field with only negative floating-point values.
			// This causes problems in our tests relative to field sorts
			// because it brings a max from -42 to 0, which changes the relative order of documents.
			// This is most likely a bug, though I couldn't find the bug report or fix,
			// and it is fixed in ES 6.x.
			// Since 5.6 is really old and EOL'd anyway, it's unlikely to ever get a fix.
			// We'll just ignore tests that fail because of this.
			return ! dialect.hasBugForSortMaxOnNegativeFloats();
		}
		else {
			return true;
		}
	}

	@Override
	public boolean supportsValuesForDynamicField(Class<?> javaType) {
		if ( BigInteger.class.equals( javaType ) ) {
			// For some reason, ES 5.6 to 7.2 fail to index BigInteger values
			// with "No matching token for number_type [BIG_INTEGER]".
			// It's fixed in Elasticsearch 7.3, though.
			return ! dialect.hasBugForBigIntegerValuesForDynamicField();
		}
		else {
			return true;
		}
	}
}
