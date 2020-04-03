/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import org.hibernate.search.engine.search.common.SortMode;

public class TckBackendFeatures {

	public boolean geoPointIndexNullAs() {
		return true;
	}

	public boolean worksFineWithStrictAboveRangedQueriesOnDecimalScaledField() {
		return true;
	}

	public boolean normalizesStringMissingValues() {
		return true;
	}

	public boolean supportsManyRoutingKeys() {
		return true;
	}

	public boolean zonedDateTimeDocValueHasUTCZoneId() {
		return false;
	}

	public boolean nonCanonicalRangeInAggregations() {
		return true;
	}

	public boolean nonDefaultOrderInTermsAggregations() {
		return true;
	}

	public boolean flushWillFailIfAppliedToDeletedIndex() {
		return true;
	}

	public boolean mergeSegmentsWillFailIfAppliedToDeletedIndex() {
		return true;
	}

	public boolean lenientOnMultiIndexesCompatibilityChecks() {
		// we decide to allow ALL the model incompatibilities Elasticsearch allows.
		return false;
	}

	public boolean fastTimeoutResolution() {
		return true;
	}

	public boolean sortByFieldValue(IndexFieldStructure indexFieldStructure, Class<?> fieldType, SortMode sortMode) {
		return true;
	}
}
