/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch;

import java.util.Map;

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

	private final String hosts;
	private final String protocol;
	private final String username;
	private final String password;
	private final boolean awsSigningEnabled;
	private final String awsSigningAccessKey;
	private final String awsSigningSecretKey;
	private final String awsSigningRegion;

	private ElasticsearchTestHostConnectionConfiguration() {
		this.hosts = System.getProperty( "test.elasticsearch.connection.hosts" );
		this.protocol = System.getProperty( "test.elasticsearch.connection.protocol" );
		this.username = System.getProperty( "test.elasticsearch.connection.username" );
		this.password = System.getProperty( "test.elasticsearch.connection.password" );
		this.awsSigningEnabled = Boolean.getBoolean( "test.elasticsearch.connection.aws.signing.enabled" );
		this.awsSigningAccessKey = System.getProperty( "test.elasticsearch.connection.aws.signing.access_key" );
		this.awsSigningSecretKey = System.getProperty( "test.elasticsearch.connection.aws.signing.secret_key" );
		this.awsSigningRegion = System.getProperty( "test.elasticsearch.connection.aws.signing.region" );

		log.infof(
				"Integration tests will connect to '%s' using protocol '%s' (AWS signing enabled: '%s')",
				hosts, protocol, awsSigningEnabled
		);
	}

	public boolean isAwsSigningEnabled() {
		return awsSigningEnabled;
	}

	public void addToBackendProperties(Map<String, Object> properties) {
		properties.put( "hosts", hosts );
		properties.put( "protocol", protocol );
		properties.put( "username", username );
		properties.put( "password", password );
		properties.put( "aws.signing.enabled", String.valueOf( awsSigningEnabled ) );
		properties.put( "aws.signing.access_key", awsSigningAccessKey );
		properties.put( "aws.signing.secret_key", awsSigningSecretKey );
		properties.put( "aws.signing.region", awsSigningRegion );
	}
}
