/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import java.lang.invoke.MethodHandles;
import java.util.OptionalInt;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch7ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch8ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.ElasticsearchModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.AmazonOpenSearchServerlessProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch70ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch80ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch81ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.ElasticsearchProtocolDialect;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Creates an Elasticsearch dialect for a given Elasticsearch version.
 */
public class ElasticsearchDialectFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Incubating
	public static final ElasticsearchVersion AMAZON_OPENSEARCH_SERVERLESS =
			ElasticsearchVersion.of( ElasticsearchDistributionName.AMAZON_OPENSEARCH_SERVERLESS, null );

	public static boolean isPreciseEnoughForModelDialect(ElasticsearchVersion version) {
		switch ( version.distribution() ) {
			case ELASTIC:
			case OPENSEARCH:
				return version.majorOptional().isPresent();
			case AMAZON_OPENSEARCH_SERVERLESS:
				return true;
			default:
				throw new AssertionFailure( "Unrecognized Elasticsearch distribution: " + version.distribution() );
		}
	}

	public static boolean isPreciseEnoughForProtocolDialect(ElasticsearchVersion version) {
		switch ( version.distribution() ) {
			case ELASTIC:
			case OPENSEARCH:
				return version.majorOptional().isPresent() && version.minor().isPresent();
			case AMAZON_OPENSEARCH_SERVERLESS:
				return true;
			default:
				throw new AssertionFailure( "Unrecognized Elasticsearch distribution: " + version.distribution() );
		}
	}

	public static boolean isVersionCheckImpossible(ElasticsearchVersion version) {
		return version.distribution().equals( ElasticsearchDistributionName.AMAZON_OPENSEARCH_SERVERLESS );
	}

	public ElasticsearchModelDialect createModelDialect(ElasticsearchVersion version) {
		switch ( version.distribution() ) {
			case ELASTIC:
				return createModelDialectElastic( version );
			case OPENSEARCH:
				return createModelDialectOpenSearch( version );
			case AMAZON_OPENSEARCH_SERVERLESS:
				return createModelDialectAmazonOpenSearchServerless( version );
			default:
				throw new AssertionFailure( "Unrecognized Elasticsearch distribution: " + version.distribution() );
		}
	}

	private ElasticsearchModelDialect createModelDialectElastic(ElasticsearchVersion version) {
		OptionalInt majorOptional = version.majorOptional();
		OptionalInt minorOptional = version.minor();
		// The major/minor version numbers should be set at this point,
		// because `isPreciseEnoughForModelDialect` was called
		// to decide whether to retrieve the version from the cluster or not.
		if ( majorOptional.isEmpty() ) {
			// The version is supposed to be fetched from the cluster itself, so it should be complete
			throw new AssertionFailure( "Cannot create the Elasticsearch model dialect because the version is incomplete." );
		}
		int major = majorOptional.getAsInt();

		if ( major < 7 || ( major == 7 && minorOptional.isPresent() && minorOptional.getAsInt() < 10 ) ) {
			throw log.unsupportedElasticsearchVersion( version );
		}
		else if ( major == 7 ) {
			return new Elasticsearch7ModelDialect();
		}
		else {
			return new Elasticsearch8ModelDialect();
		}
	}

	private ElasticsearchModelDialect createModelDialectOpenSearch(ElasticsearchVersion version) {
		OptionalInt majorOptional = version.majorOptional();
		OptionalInt minorOptional = version.minor();
		// The major/minor version numbers should be set at this point,
		// because `isPreciseEnoughForModelDialect` was called
		// to decide whether to retrieve the version from the cluster or not.
		if ( majorOptional.isEmpty() ) {
			// The version is supposed to be fetched from the cluster itself, so it should be complete
			throw new AssertionFailure( "Cannot create the OpenSearch model dialect because the version is incomplete." );
		}
		int major = majorOptional.getAsInt();

		if ( major < 1 || ( major == 1 && minorOptional.isPresent() && minorOptional.getAsInt() < 3 ) ) {
			throw log.unsupportedElasticsearchVersion( version );
		}
		else {
			return new Elasticsearch7ModelDialect();
		}
	}

	private ElasticsearchModelDialect createModelDialectAmazonOpenSearchServerless(ElasticsearchVersion version) {
		if ( !AMAZON_OPENSEARCH_SERVERLESS.equals( version ) ) {
			throw log.unexpectedAwsOpenSearchServerlessVersion( version, AMAZON_OPENSEARCH_SERVERLESS );
		}
		return new Elasticsearch7ModelDialect();
	}

	public ElasticsearchProtocolDialect createProtocolDialect(ElasticsearchVersion version) {
		switch ( version.distribution() ) {
			case ELASTIC:
				return createProtocolDialectElastic( version );
			case OPENSEARCH:
				return createProtocolDialectOpenSearch( version );
			case AMAZON_OPENSEARCH_SERVERLESS:
				return createProtocolDialectAmazonOpenSearchServerless( version );
			default:
				throw new AssertionFailure( "Unrecognized Elasticsearch distribution: " + version.distribution() );
		}
	}

	private ElasticsearchProtocolDialect createProtocolDialectElastic(ElasticsearchVersion version) {
		OptionalInt majorOptional = version.majorOptional();
		OptionalInt minorOptional = version.minor();
		// The major/minor version numbers should be set at this point,
		// because `isPreciseEnoughForProtocolDialect` was called
		// to decide whether to retrieve the version from the cluster or not.
		if ( majorOptional.isEmpty() || minorOptional.isEmpty() ) {
			// The version is supposed to be fetched from the cluster itself, so it should be complete
			throw new AssertionFailure( "Cannot create the Elasticsearch protocol dialect because the version is incomplete." );
		}
		int major = majorOptional.getAsInt();
		int minor = minorOptional.getAsInt();

		if ( major < 7 || ( major == 7 && minor < 10 ) ) {
			throw log.unsupportedElasticsearchVersion( version );
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

	private ElasticsearchProtocolDialect createProtocolDialectElasticV7(ElasticsearchVersion version, int minor) {
		if ( minor > 17 ) {
			log.unknownElasticsearchVersion( version );
		}
		return new Elasticsearch70ProtocolDialect();
	}

	private ElasticsearchProtocolDialect createProtocolDialectElasticV8(ElasticsearchVersion version, int minor) {
		if ( minor > 9 ) {
			log.unknownElasticsearchVersion( version );
		}
		else if ( minor == 0 ) {
			return new Elasticsearch80ProtocolDialect();
		}
		return new Elasticsearch81ProtocolDialect();
	}

	private ElasticsearchProtocolDialect createProtocolDialectOpenSearch(ElasticsearchVersion version) {
		OptionalInt majorOptional = version.majorOptional();
		OptionalInt minorOptional = version.minor();
		// The major/minor version numbers should be set at this point,
		// because `isPreciseEnoughForProtocolDialect` was called
		// to decide whether to retrieve the version from the cluster or not.
		if ( majorOptional.isEmpty() || minorOptional.isEmpty() ) {
			// The version is supposed to be fetched from the cluster itself, so it should be complete
			throw new AssertionFailure( "Cannot create the OpenSearch protocol dialect because the version is incomplete." );
		}
		int major = majorOptional.getAsInt();
		int minor = minorOptional.getAsInt();

		if ( major < 1 || ( major == 1 && minor < 3 ) ) {
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
		if ( minor > 10 ) {
			log.unknownElasticsearchVersion( version );
		}
		return new Elasticsearch70ProtocolDialect();
	}

	private ElasticsearchProtocolDialect createProtocolDialectAmazonOpenSearchServerless(ElasticsearchVersion version) {
		if ( !AMAZON_OPENSEARCH_SERVERLESS.equals( version ) ) {
			throw log.unexpectedAwsOpenSearchServerlessVersion( version, AMAZON_OPENSEARCH_SERVERLESS );
		}
		return new AmazonOpenSearchServerlessProtocolDialect();
	}
}
