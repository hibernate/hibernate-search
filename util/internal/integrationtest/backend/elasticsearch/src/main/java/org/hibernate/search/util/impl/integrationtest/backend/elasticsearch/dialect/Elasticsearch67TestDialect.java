/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect;

public class Elasticsearch67TestDialect extends Elasticsearch68TestDialect {

	@Override
	public boolean ignoresFieldSortWhenNestedFieldMissing() {
		// Support for ignoring field sorts when a nested field is missing was added in 6.8.1/7.1.2:
		// https://github.com/elastic/elasticsearch/pull/42451
		// In 6.8.0 and below, we just can't ignore unmapped nested fields in field sorts,
		// which means sorts will fail when the nested field is not present in all indexes.
		return false;
	}

}
