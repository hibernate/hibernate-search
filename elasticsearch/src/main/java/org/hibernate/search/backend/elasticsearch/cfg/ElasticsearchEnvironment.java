/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

/**
 * Configuration properties of the ES backend.
 *
 * @author Gunnar Morling
 */
public final class ElasticsearchEnvironment {

	public static final String SERVER_URI = "hibernate.search.elasticsearch.host";
	public static final String INDEX_MANAGEMENT_STRATEGY = "hibernate.search.elasticsearch.index_management_strategy";

	private ElasticsearchEnvironment() {
	}
}
