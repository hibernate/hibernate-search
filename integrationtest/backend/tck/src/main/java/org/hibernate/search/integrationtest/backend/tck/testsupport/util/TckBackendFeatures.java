/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

public class TckBackendFeatures {

	public boolean distanceSortDesc() {
		return true;
	}

	public boolean geoPointIndexNullAs() {
		return true;
	}

	public boolean worksFineWithStrictAboveRangedQueriesOnDecimalScaledField() {
		return true;
	}

	public boolean normalizeStringMissingValues() {
		return true;
	}

	public boolean supportsManyRoutingKeys() {
		return true;
	}

	public boolean zonedDateTimeDocValueHasUTCZoneId() {
		return false;
	}

	public boolean aggregationsOnMultiValuedFields(Class<?> fieldType) {
		return true;
	}

	public boolean nonCanonicalRangeInAggregations() {
		return true;
	}

	public boolean nonDefaultOrderInTermsAggregations() {
		return true;
	}
}
