/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.dialect.impl;

import java.util.Properties;

import org.elasticsearch.client.RestClient;
import org.hibernate.search.elasticsearch.dialect.impl.es2.Elasticsearch2Dialect;

/**
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchDialectFactory implements ElasticsearchDialectFactory {

	@Override
	public ElasticsearchDialect createDialect(RestClient client, Properties properties) {
		return new Elasticsearch2Dialect();
	}
}
