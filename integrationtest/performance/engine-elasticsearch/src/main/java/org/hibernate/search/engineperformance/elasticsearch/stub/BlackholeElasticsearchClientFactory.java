/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch.stub;

import java.util.Properties;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClientFactory;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClientImplementor;

/**
 * @author Yoann Rodiere
 */
public class BlackholeElasticsearchClientFactory implements ElasticsearchClientFactory {

	private final String elasticsearchVersion;

	public BlackholeElasticsearchClientFactory(String elasticsearchVersion) {
		super();
		this.elasticsearchVersion = elasticsearchVersion;
	}

	@Override
	public ElasticsearchClientImplementor create(Properties properties) {
		return new BlackholeElasticsearchClient( elasticsearchVersion );
	}

}
