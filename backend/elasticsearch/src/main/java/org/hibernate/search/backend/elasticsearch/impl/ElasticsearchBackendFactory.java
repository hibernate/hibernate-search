/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.Properties;

import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.client.impl.StubElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.StubElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.spi.Backend;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.common.spi.BuildContext;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchBackendFactory implements BackendFactory {

	@Override
	public Backend<?> create(String name, BuildContext context, Properties properties) {
		// TODO more checks on the host (non-null, non-empty)
		String host = properties.getProperty( "host" );
		// TODO implement and detect dialects
		ElasticsearchClient client = new StubElasticsearchClient( host );
		ElasticsearchWorkFactory workFactory = new StubElasticsearchWorkFactory();
		return new ElasticsearchBackend( client, name, workFactory );
	}

}
