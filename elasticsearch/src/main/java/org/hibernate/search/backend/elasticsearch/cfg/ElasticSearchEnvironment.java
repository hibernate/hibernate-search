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
public final class ElasticSearchEnvironment {

	public static final String SERVER_URI = "org.hibernate.search.elasticsearch.host";

	private ElasticSearchEnvironment() {
	}
}
