/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.dialect.impl;

import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;

/**
 * An entry point to all operations that may be implemented differently depending
 * on the Elasticsearch version running on the Elasticsearch cluster.
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchDialect {

	GsonProvider getGsonProvider();

	ElasticsearchWorkFactory getWorkFactory();

}
