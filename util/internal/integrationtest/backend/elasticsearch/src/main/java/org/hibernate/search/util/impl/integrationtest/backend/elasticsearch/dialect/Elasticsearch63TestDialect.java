/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

public class Elasticsearch63TestDialect extends Elasticsearch64TestDialect {

	@Override
	public boolean supportsIsWriteIndex() {
		return false;
	}

	@Override
	public boolean supportsIgnoreUnmappedForGeoPointField() {
		// Support for ignore_unmapped in geo_distance sorts added in 6.4:
		// https://github.com/elastic/elasticsearch/pull/31153
		// In 6.3 and below, we just can't ignore unmapped fields,
		// which means sorts will fail when the geo_point field is not present in all indexes.
		return false;
	}
}
