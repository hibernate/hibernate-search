/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.filter;

/**
 * Simple implementation of a native Elasticsearch full text filter.
 *
 * It only allows to define a static filter by calling the constructor.
 *
 * @author Guillaume Smet
 */
public class SimpleElasticsearchFilter implements ElasticsearchFilter {

	private String jsonFilter;

	public SimpleElasticsearchFilter(String jsonFilter) {
		this.jsonFilter = jsonFilter;
	}

	@Override
	public String getJsonFilter() {
		return jsonFilter;
	}

}
