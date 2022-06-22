/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.testsupport;

import static org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration.BACKEND_TYPE;

import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;

public class BackendConfigurations {

	private BackendConfigurations() {
	}

	public static BackendConfiguration simple() {
		switch ( BACKEND_TYPE ) {
			case "lucene":
				return new LuceneBackendConfiguration();
			case "elasticsearch":
				return new ElasticsearchBackendConfiguration();
			default:
				throw new IllegalStateException( "Unknown backend type:" + BACKEND_TYPE );
		}
	}

}
