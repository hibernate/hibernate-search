/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg.spi;

/**
 * Implementation-related settings, used for testing only.
 *
 * @deprecated Use {@link org.hibernate.search.backend.elasticsearch.cfg.impl.ElasticsearchBackendImplSettings} instead.
 */
@Deprecated
public final class ElasticsearchBackendImplSettings {

	private ElasticsearchBackendImplSettings() {
	}

	public static final String CLIENT_FACTORY = "client_factory";

}
