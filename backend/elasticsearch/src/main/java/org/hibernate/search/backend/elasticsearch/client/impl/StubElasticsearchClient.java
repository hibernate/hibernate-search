/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;


/**
 * @author Yoann Rodiere
 */
public class StubElasticsearchClient implements ElasticsearchClient {

	private final String host;

	public StubElasticsearchClient(String host) {
		this.host = host;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + host + "]";
	}

}
