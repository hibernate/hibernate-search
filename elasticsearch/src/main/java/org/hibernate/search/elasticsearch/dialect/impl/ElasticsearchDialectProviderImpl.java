/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.dialect.impl;

import java.util.Properties;

import org.hibernate.search.elasticsearch.dialect.impl.es2.Elasticsearch2Dialect;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.BuildContext;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchDialectProviderImpl implements ElasticsearchDialectProvider, Startable, Stoppable {

	private ElasticsearchDialectImplementor dialect;

	@Override
	public void start(Properties properties, BuildContext context) {
		this.dialect = new Elasticsearch2Dialect();
	}

	@Override
	public void stop() {
		try {
			this.dialect.close();
			this.dialect = null;
		}
		catch (RuntimeException e) {
			throw new SearchException( "Failed to shut down the Elasticsearch dialect", e );
		}
	}

	@Override
	public ElasticsearchDialect getDialect() {
		return dialect;
	}
}
