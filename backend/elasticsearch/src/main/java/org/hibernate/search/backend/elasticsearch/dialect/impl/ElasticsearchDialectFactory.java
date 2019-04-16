/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Allows to create an Elasticsearch dialect by detecting the version of a remote cluster.
 */
public class ElasticsearchDialectFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ElasticsearchDialect create(ElasticsearchVersion version) {
		if ( version.getMajor() < 5 ) {
			throw log.unsupportedElasticsearchVersion( version );
		}
		else if ( version.getMajor() == 5 ) {
			if ( version.getMinor() < 6 ) {
				throw log.unsupportedElasticsearchVersion( version );
			}
			// Either the latest supported version, or a newer/unknown one
			if ( version.getMinor() != 6 ) {
				log.unknownElasticsearchVersion( version );
			}
			return new Elasticsearch56Dialect();
		}
		else if ( version.getMajor() == 6 ) {
			if ( version.getMinor() < 7 ) {
				return new Elasticsearch60Dialect();
			}
			// Either the latest supported version, or a newer/unknown one
			if ( version.getMinor() != 7 ) {
				log.unknownElasticsearchVersion( version );
			}
			return new Elasticsearch67Dialect();
		}
		else {
			// Either the latest supported version, or a newer/unknown one
			if ( version.getMajor() != 7 ) {
				log.unknownElasticsearchVersion( version );
			}
			return new Elasticsearch7Dialect();
		}
	}

}
