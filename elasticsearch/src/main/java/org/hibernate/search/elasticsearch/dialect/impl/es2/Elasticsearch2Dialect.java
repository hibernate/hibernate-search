/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.dialect.impl.es2;

import java.util.Properties;

import org.hibernate.search.elasticsearch.dialect.impl.ElasticsearchDialectImplementor;
import org.hibernate.search.elasticsearch.work.impl.factory.Elasticsearch2WorkFactory;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactoryImplementor;
import org.hibernate.search.spi.BuildContext;

/**
 * @author Yoann Rodiere
 */
public class Elasticsearch2Dialect implements ElasticsearchDialectImplementor {

	private ElasticsearchWorkFactoryImplementor workFactory;

	@Override
	public void init(Properties properties, BuildContext context) {
		this.workFactory = instantiateWorkFactory();
		this.workFactory.init( properties, context );
	}

	protected ElasticsearchWorkFactoryImplementor instantiateWorkFactory() {
		return new Elasticsearch2WorkFactory();
	}

	@Override
	public void close() {
		this.workFactory.close();
		this.workFactory = null;
	}

	@Override
	public ElasticsearchWorkFactory getWorkFactory() {
		return workFactory;
	}

}
