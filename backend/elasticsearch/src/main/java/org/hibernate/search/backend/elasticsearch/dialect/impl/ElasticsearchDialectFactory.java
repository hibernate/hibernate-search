/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import java.lang.invoke.MethodHandles;
import java.util.OptionalInt;

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch56ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch6ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch7ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch8ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.ElasticsearchModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch56ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch60ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch63ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch64ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch67ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch70ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch80ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch81ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.ElasticsearchProtocolDialect;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Creates an Elasticsearch dialect for a given Elasticsearch version.
 */
public class ElasticsearchDialectFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ElasticsearchModelDialect createModelDialect(ElasticsearchVersion version) {
		switch ( version.distribution() ) {
			case ELASTIC:
				return createModelDialectElastic( version );
			case OPENSEARCH:
				return createModelDialectOpenSearch( version );
			default:
				throw new AssertionFailure( "Unrecognized Elasticsearch distribution: " + version.distribution() );
		}
	}

	private ElasticsearchModelDialect createModelDialectElastic(ElasticsearchVersion version) {
		int major = version.major();

		if ( major < 5 ) {
			throw log.unsupportedElasticsearchVersion( version );
		}
		else if ( major == 5 ) {
			return createModelDialectElasticV5( version );
		}
		else if ( major == 6 ) {
			return new Elasticsearch6ModelDialect();
		}
		else if ( major == 7 ) {
			return new Elasticsearch7ModelDialect();
		}
		else {
			return new Elasticsearch8ModelDialect();
		}
	}

	private ElasticsearchModelDialect createModelDialectElasticV5(ElasticsearchVersion version) {
		OptionalInt minorOptional = version.minor();
		if ( !minorOptional.isPresent() ) {
			throw log.ambiguousElasticsearchVersion( version );
		}
		int minor = minorOptional.getAsInt();
		if ( minor < 6 ) {
			throw log.unsupportedElasticsearchVersion( version );
		}
		return new Elasticsearch56ModelDialect();
	}

	private ElasticsearchModelDialect createModelDialectOpenSearch(ElasticsearchVersion version) {
		int major = version.major();

		if ( major < 1 ) {
			throw log.unsupportedElasticsearchVersion( version );
		}
		else {
			return new Elasticsearch7ModelDialect();
		}
	}

	public ElasticsearchProtocolDialect createProtocolDialect(ElasticsearchVersion version) {
		switch ( version.distribution() ) {
			case ELASTIC:
				return createProtocolDialectElastic( version );
			case OPENSEARCH:
				return createProtocolDialectOpenSearch( version );
			default:
				throw new AssertionFailure( "Unrecognized Elasticsearch distribution: " + version.distribution() );
		}
	}

	private ElasticsearchProtocolDialect createProtocolDialectElastic(ElasticsearchVersion version) {
		int major = version.major();
		OptionalInt minorOptional = version.minor();
		if ( !minorOptional.isPresent() ) {
			// The version is supposed to be fetched from the cluster itself, so it should be complete
			throw new AssertionFailure( "The Elasticsearch version is incomplete when creating the protocol dialect." );
		}
		int minor = minorOptional.getAsInt();

		if ( major < 5 ) {
			throw log.unsupportedElasticsearchVersion( version );
		}
		else if ( major == 5 ) {
			return createProtocolDialectElasticV5( version, minor );
		}
		else if ( major == 6 ) {
			return createProtocolDialectElasticV6( version, minor );
		}
		else if ( major == 7 ) {
			return createProtocolDialectElasticV7( version, minor );
		}
		else if ( major == 8 ) {
			return createProtocolDialectElasticV8( version, minor );
		}
		else {
			log.unknownElasticsearchVersion( version );
			return new Elasticsearch81ProtocolDialect();
		}
	}

	private ElasticsearchProtocolDialect createProtocolDialectElasticV5(ElasticsearchVersion version, int minor) {
		if ( minor < 6 ) {
			throw log.unsupportedElasticsearchVersion( version );
		}
		// Either the latest supported version, or a newer/unknown one
		if ( minor != 6 ) {
			log.unknownElasticsearchVersion( version );
		}
		return new Elasticsearch56ProtocolDialect();
	}

	private ElasticsearchProtocolDialect createProtocolDialectElasticV6(ElasticsearchVersion version, int minor) {
		if ( minor < 3 ) {
			return new Elasticsearch60ProtocolDialect();
		}
		if ( minor < 4 ) {
			return new Elasticsearch63ProtocolDialect();
		}
		if ( minor < 7 ) {
			return new Elasticsearch64ProtocolDialect();
		}
		// Either the latest supported version, or a newer/unknown one
		if ( minor > 8 ) {
			log.unknownElasticsearchVersion( version );
		}
		return new Elasticsearch67ProtocolDialect();
	}

	private ElasticsearchProtocolDialect createProtocolDialectElasticV7(ElasticsearchVersion version, int minor) {
		if ( minor > 17 ) {
			log.unknownElasticsearchVersion( version );
		}
		return new Elasticsearch70ProtocolDialect();
	}

	private ElasticsearchProtocolDialect createProtocolDialectElasticV8(ElasticsearchVersion version, int minor) {
		if ( minor > 6 ) {
			log.unknownElasticsearchVersion( version );
		}
		else if ( minor == 0 ) {
			return new Elasticsearch80ProtocolDialect();
		}
		return new Elasticsearch81ProtocolDialect();
	}

	private ElasticsearchProtocolDialect createProtocolDialectOpenSearch(ElasticsearchVersion version) {
		int major = version.major();
		OptionalInt minorOptional = version.minor();
		if ( !minorOptional.isPresent() ) {
			// The version is supposed to be fetched from the cluster itself, so it should be complete
			throw new AssertionFailure( "The Elasticsearch version is incomplete when creating the protocol dialect." );
		}
		int minor = minorOptional.getAsInt();
		if ( major < 1 ) {
			throw log.unsupportedElasticsearchVersion( version );
		}
		else if ( major == 1 ) {
			return createProtocolDialectOpenSearchV1( version, minor );
		}
		else if ( major == 2 ) {
			return createProtocolDialectOpenSearchV2( version, minor );
		}
		else {
			log.unknownElasticsearchVersion( version );
			return new Elasticsearch70ProtocolDialect();
		}
	}

	private ElasticsearchProtocolDialect createProtocolDialectOpenSearchV1(ElasticsearchVersion version, int minor) {
		if ( minor > 3 ) {
			log.unknownElasticsearchVersion( version );
		}
		return new Elasticsearch70ProtocolDialect();
	}

	private ElasticsearchProtocolDialect createProtocolDialectOpenSearchV2(ElasticsearchVersion version, int minor) {
		if ( minor > 6 ) {
			log.unknownElasticsearchVersion( version );
		}
		return new Elasticsearch70ProtocolDialect();
	}

}
