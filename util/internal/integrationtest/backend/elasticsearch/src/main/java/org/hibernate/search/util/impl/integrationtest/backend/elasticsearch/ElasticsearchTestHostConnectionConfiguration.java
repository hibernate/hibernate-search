/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;

import org.jboss.logging.Logger;

public class ElasticsearchTestHostConnectionConfiguration {

	private static final Logger log = Logger.getLogger( ElasticsearchTestHostConnectionConfiguration.class.getName() );

	private static ElasticsearchTestHostConnectionConfiguration instance;

	public static ElasticsearchTestHostConnectionConfiguration get() {
		if ( instance == null ) {
			instance = new ElasticsearchTestHostConnectionConfiguration();
		}
		return instance;
	}

	private final String uris;
	private final String username;
	private final String password;
	private final Boolean awsSigningEnabled;
	private final String awsRegion;
	private final String awsCredentialsType;
	private final String awsCredentialsAccessKeyId;
	private final String awsCredentialsSecretAccessKey;

	private ElasticsearchTestHostConnectionConfiguration() {
		this.uris = System.getProperty( "test.elasticsearch.connection.uris" );
		this.username = System.getProperty( "test.elasticsearch.connection.username" );
		this.password = System.getProperty( "test.elasticsearch.connection.password" );
		String enabledAsString = System.getProperty( "test.elasticsearch.connection.aws.signing.enabled" );
		this.awsSigningEnabled = enabledAsString == null ? null : Boolean.parseBoolean( enabledAsString );
		this.awsRegion = System.getProperty( "test.elasticsearch.connection.aws.region" );
		this.awsCredentialsType = System.getProperty( "test.elasticsearch.connection.aws.credentials.type" );
		this.awsCredentialsAccessKeyId = System.getProperty( "test.elasticsearch.connection.aws.credentials.access_key_id" );
		this.awsCredentialsSecretAccessKey = System.getProperty( "test.elasticsearch.connection.aws.credentials.secret_access_key" );

		log.infof(
				"Integration tests will connect to '%s' (AWS signing enabled: '%s')",
				uris, awsSigningEnabled
		);
	}

	public boolean isAws() {
		return awsSigningEnabled != null && awsSigningEnabled;
	}

	public void addToBackendProperties(Map<String, ? super String> properties) {
		properties.put( "uris", uris );
		properties.put( "username", username );
		properties.put( "password", password );
		properties.put( "aws.signing.enabled", awsSigningEnabled == null ? null : awsSigningEnabled.toString() );
		properties.put( "aws.region", awsRegion );
		properties.put( "aws.credentials.type", awsCredentialsType );
		properties.put( "aws.credentials.access_key_id", awsCredentialsAccessKeyId );
		properties.put( "aws.credentials.secret_access_key", awsCredentialsSecretAccessKey );
		if ( isAws() ) {
			// AWS Elasticsearch Service is (sometimes) super slow for index creation.
			// Just raise the default timeout so that we don't fail a full 30-min
			// test run just for one small freeze.
			properties.put( ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT, "60000" );
		}
	}
}
