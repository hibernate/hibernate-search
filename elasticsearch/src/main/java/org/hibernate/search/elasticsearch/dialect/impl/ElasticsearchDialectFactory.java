/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.dialect.impl;

import java.util.Properties;

import org.elasticsearch.client.RestClient;
import org.hibernate.search.engine.service.spi.Service;


/**
 * A service allowing to create an Elasticserach dialect to use with a given client.
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchDialectFactory extends Service {

	ElasticsearchDialect createDialect(RestClient client, Properties properties);

}
