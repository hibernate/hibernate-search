/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchDialectName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Allows to create an Elasticsearch dialect by detecting the version of a remote cluster.
 */
public class ElasticsearchDialectFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ElasticsearchDialect create(ElasticsearchDialectName dialectName) {
		switch ( dialectName ) {
			case ES_5_6:
				return new Elasticsearch56Dialect();
			case ES_6:
				return new Elasticsearch60Dialect();
			case ES_6_7:
				return new Elasticsearch67Dialect();
			case ES_7:
				return new Elasticsearch7Dialect();
			case AUTO:
			default:
				throw new AssertionFailure( "Unexpected dialect name in the create() method: " + dialectName );
		}
	}

	public ElasticsearchDialectName getAppropriateDialectName(ElasticsearchVersion version) {
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
			return ElasticsearchDialectName.ES_5_6;
		}
		else if ( version.getMajor() == 6 ) {
			if ( version.getMinor() < 7 ) {
				return ElasticsearchDialectName.ES_6;
			}
			// Either the latest supported version, or a newer/unknown one
			if ( version.getMinor() != 7 ) {
				log.unknownElasticsearchVersion( version );
			}
			return ElasticsearchDialectName.ES_6_7;
		}
		else {
			// Either the latest supported version, or a newer/unknown one
			if ( version.getMajor() != 7 ) {
				log.unknownElasticsearchVersion( version );
			}
			return ElasticsearchDialectName.ES_7;
		}
	}

	public void checkAppropriate(ElasticsearchDialectName configuredDialectName, ElasticsearchVersion version) {
		ElasticsearchDialectName appropriateDialectName = getAppropriateDialectName( version );
		if ( !appropriateDialectName.equals( configuredDialectName ) ) {
			throw log.unexpectedElasticsearchVersion( version, appropriateDialectName, configuredDialectName );
		}
	}

}
