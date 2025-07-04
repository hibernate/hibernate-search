/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.dialect.impl;

import java.util.OptionalInt;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch7ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch812ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch814ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.Elasticsearch8ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.ElasticsearchModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.OpenSearch1ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.OpenSearch214ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.OpenSearch29ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.model.impl.OpenSearch2ModelDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.AmazonOpenSearchServerlessProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch70ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch80ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.Elasticsearch81ProtocolDialect;
import org.hibernate.search.backend.elasticsearch.dialect.protocol.impl.ElasticsearchProtocolDialect;
import org.hibernate.search.backend.elasticsearch.logging.impl.VersionLog;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Creates an Elasticsearch dialect for a given Elasticsearch version.
 */
public class ElasticsearchDialectFactory {

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
			throw VersionLog.INSTANCE.unsupportedElasticsearchVersion( version );
		}
		else if ( major == 7 ) {
			return new Elasticsearch7ModelDialect();
		}
		else {
			// if there's no minor -- who knows which version it is, better stay safe
			// and assume that only 8 features are available, and nothing from 8.12+
			if ( major == 8 && ( minorOptional.isEmpty() || minorOptional.getAsInt() < 12 ) ) {
				return new Elasticsearch8ModelDialect();
			}
			if ( major == 8 && minorOptional.getAsInt() < 14 ) {
				return new Elasticsearch812ModelDialect();
			}

			return new Elasticsearch814ModelDialect();
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
			throw VersionLog.INSTANCE.unsupportedElasticsearchVersion( version );
		}
		else if ( major == 1 ) {
			return new OpenSearch1ModelDialect();
		}
		else {
			if ( major == 2 ) {
				if ( ( minorOptional.isEmpty() || minorOptional.getAsInt() < 9 ) ) {
					return new OpenSearch2ModelDialect();
				}

				if ( ( minorOptional.getAsInt() < 14 ) ) {
					return new OpenSearch29ModelDialect();
				}
			}
			if ( major == 3 ) {
				// While at the moment this if is redundant, it is more of a placeholder
				return new OpenSearch214ModelDialect();
			}
			return new OpenSearch214ModelDialect();
		}
	}

	private ElasticsearchModelDialect createModelDialectAmazonOpenSearchServerless(ElasticsearchVersion version) {
		if ( !AMAZON_OPENSEARCH_SERVERLESS.equals( version ) ) {
			throw VersionLog.INSTANCE.unexpectedAwsOpenSearchServerlessVersion( version, AMAZON_OPENSEARCH_SERVERLESS );
		}
		return new OpenSearch214ModelDialect();
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
			throw VersionLog.INSTANCE.unsupportedElasticsearchVersion( version );
		}
		else if ( major == 7 ) {
			return createProtocolDialectElasticV7( version, minor );
		}
		else if ( major == 8 ) {
			return createProtocolDialectElasticV8( version, minor );
		}
		else if ( major == 9 ) {
			return createProtocolDialectElasticV9( version, minor );
		}
		else {
			VersionLog.INSTANCE.unknownElasticsearchVersion( version );
			return new Elasticsearch81ProtocolDialect();
		}
	}

	private ElasticsearchProtocolDialect createProtocolDialectElasticV7(ElasticsearchVersion version, int minor) {
		if ( minor > 17 ) {
			VersionLog.INSTANCE.unknownElasticsearchVersion( version );
		}
		return new Elasticsearch70ProtocolDialect();
	}

	private ElasticsearchProtocolDialect createProtocolDialectElasticV8(ElasticsearchVersion version, int minor) {
		if ( minor > 18 ) {
			VersionLog.INSTANCE.unknownElasticsearchVersion( version );
		}
		else if ( minor == 0 ) {
			return new Elasticsearch80ProtocolDialect();
		}
		return new Elasticsearch81ProtocolDialect();
	}

	private ElasticsearchProtocolDialect createProtocolDialectElasticV9(ElasticsearchVersion version, int minor) {
		if ( minor > 0 ) {
			VersionLog.INSTANCE.unknownElasticsearchVersion( version );
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
			throw VersionLog.INSTANCE.unsupportedElasticsearchVersion( version );
		}
		else if ( major == 1 ) {
			return createProtocolDialectOpenSearchV1( version, minor );
		}
		else if ( major == 2 ) {
			return createProtocolDialectOpenSearchV2( version, minor );
		}
		else if ( major == 3 ) {
			return createProtocolDialectOpenSearchV3( version, minor );
		}
		else {
			VersionLog.INSTANCE.unknownElasticsearchVersion( version );
			return new Elasticsearch70ProtocolDialect();
		}
	}

	private ElasticsearchProtocolDialect createProtocolDialectOpenSearchV1(ElasticsearchVersion version, int minor) {
		if ( minor > 3 ) {
			VersionLog.INSTANCE.unknownElasticsearchVersion( version );
		}
		return new Elasticsearch70ProtocolDialect();
	}

	private ElasticsearchProtocolDialect createProtocolDialectOpenSearchV2(ElasticsearchVersion version, int minor) {
		if ( minor > 19 ) {
			VersionLog.INSTANCE.unknownElasticsearchVersion( version );
		}
		return new Elasticsearch70ProtocolDialect();
	}

	private ElasticsearchProtocolDialect createProtocolDialectOpenSearchV3(ElasticsearchVersion version, int minor) {
		if ( minor > 1 ) {
			VersionLog.INSTANCE.unknownElasticsearchVersion( version );
		}
		return new Elasticsearch70ProtocolDialect();
	}

	private ElasticsearchProtocolDialect createProtocolDialectAmazonOpenSearchServerless(ElasticsearchVersion version) {
		if ( !AMAZON_OPENSEARCH_SERVERLESS.equals( version ) ) {
			throw VersionLog.INSTANCE.unexpectedAwsOpenSearchServerlessVersion( version, AMAZON_OPENSEARCH_SERVERLESS );
		}
		return new AmazonOpenSearchServerlessProtocolDialect();
	}
}
