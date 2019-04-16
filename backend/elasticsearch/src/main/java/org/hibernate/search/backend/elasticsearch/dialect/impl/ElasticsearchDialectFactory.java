/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import java.lang.invoke.MethodHandles;
import java.util.OptionalInt;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Allows to create an Elasticsearch dialect by detecting the version of a remote cluster.
 */
public class ElasticsearchDialectFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ElasticsearchDialect create(ElasticsearchVersion version) {
		int major = version.getMajor();
		OptionalInt minorOptional = version.getMinor();

		if ( major < 5 ) {
			throw log.unsupportedElasticsearchVersion( version );
		}
		else if ( major == 5 ) {
			if ( !minorOptional.isPresent() ) {
				throw log.ambiguousElasticsearchVersion( version );
			}
			int minor = minorOptional.getAsInt();
			if ( minor < 6 ) {
				throw log.unsupportedElasticsearchVersion( version );
			}
			// Either the latest supported version, or a newer/unknown one
			if ( minor != 6 ) {
				log.unknownElasticsearchVersion( version );
			}
			return new Elasticsearch56Dialect();
		}
		else if ( major == 6 ) {
			// FIXME allow to just use "6" as a version by splitting the dialect in two (model dialect and protocol dialect) and auto-detecting the protocol dialect
			if ( !minorOptional.isPresent() ) {
				throw log.ambiguousElasticsearchVersion( version );
			}
			int minor = minorOptional.getAsInt();
			if ( minor < 7 ) {
				return new Elasticsearch60Dialect();
			}
			// Either the latest supported version, or a newer/unknown one
			if ( minor != 7 ) {
				log.unknownElasticsearchVersion( version );
			}
			return new Elasticsearch67Dialect();
		}
		else {
			// Either the latest supported version, or a newer/unknown one
			if ( major != 7 ) {
				log.unknownElasticsearchVersion( version );
			}
			return new Elasticsearch7Dialect();
		}
	}

}
