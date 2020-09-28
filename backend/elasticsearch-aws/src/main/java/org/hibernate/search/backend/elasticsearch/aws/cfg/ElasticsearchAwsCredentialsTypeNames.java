/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.aws.cfg;

public final class ElasticsearchAwsCredentialsTypeNames {

	private ElasticsearchAwsCredentialsTypeNames() {
	}

	/**
	 * Use the default credentials from the AWS SDK.
	 *
	 * @see software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
	 */
	public static final String DEFAULT = "default";

	/**
	 * Use static credentials, to be provided using the configuration properties
	 * {@link ElasticsearchAwsBackendSettings#CREDENTIALS_ACCESS_KEY_ID}
	 * and {@link ElasticsearchAwsBackendSettings#CREDENTIALS_SECRET_ACCESS_KEY}.
	 *
	 * @see software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
	 */
	public static final String STATIC = "static";

}
