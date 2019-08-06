/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.elasticsearch;

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

	private final String url;
	private final String username;
	private final String password;
	private final boolean awsSigningEnabled;
	private final String awsSigningAccessKey;
	private final String awsSigningSecretKey;
	private final String awsSigningRegion;

	private ElasticsearchTestHostConnectionConfiguration() {
		this.url = System.getProperty( "test.elasticsearch.host.url" );
		this.username = System.getProperty( "test.elasticsearch.host.username" );
		this.password = System.getProperty( "test.elasticsearch.host.password" );
		this.awsSigningEnabled = Boolean.getBoolean( "test.elasticsearch.host.aws.signing.enabled" );
		this.awsSigningAccessKey = System.getProperty( "test.elasticsearch.host.aws.signing.access_key" );
		this.awsSigningSecretKey = System.getProperty( "test.elasticsearch.host.aws.signing.secret_key" );
		this.awsSigningRegion = System.getProperty( "test.elasticsearch.host.aws.signing.region" );

		log.infof(
				"Integration tests will connect to '%s' (AWS signing enabled: '%s')",
				url, awsSigningEnabled
		);
	}

	public boolean isAwsSigningEnabled() {
		return awsSigningEnabled;
	}

	public void addToBackendProperties(Map<String, Object> properties) {
		properties.put( "hosts", url );
		properties.put( "username", username );
		properties.put( "password", password );
		properties.put( "aws.signing.enabled", String.valueOf( awsSigningEnabled ) );
		properties.put( "aws.signing.access_key", awsSigningAccessKey );
		properties.put( "aws.signing.secret_key", awsSigningSecretKey );
		properties.put( "aws.signing.region", awsSigningRegion );
	}
}
