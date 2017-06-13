/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test.filter;

import org.hibernate.search.elasticsearch.filter.ElasticsearchFilter;

public class DriversMatchingNameElasticsearchFilter implements ElasticsearchFilter {

	private String name;

	public DriversMatchingNameElasticsearchFilter() {
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getJsonFilter() {
		return "{ 'term': { 'name': '" + name + "' } }";
	}

}
