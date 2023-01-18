/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.aws.cfg;


import org.hibernate.search.util.common.impl.HibernateSearchConfiguration;

/**
 * AWS-specific configuration properties for Elasticsearch backends.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key;
 * see {@link org.hibernate.search.engine.cfg.BackendSettings} for details.
 */
@HibernateSearchConfiguration(
		prefix = { "hibernate.search.backend.", "hibernate.search.backends.<backend name>." },
		title = "Hibernate Search Backend - Elasticsearch - AWS integration",
		anchorPrefix = "hibernate-search-backend-elasticsearch-aws-"
)
public final class ElasticsearchAwsBackendSettings {

	private ElasticsearchAwsBackendSettings() {
	}

	/**
	 * Whether requests should be signed using the AWS credentials.
	 * <p>
	 * Expects a Boolean value such as {@code true} or {@code false},
	 * or a string that can be parsed to such Boolean value.
	 * <p>
	 * Defaults to {@link Defaults#SIGNING_ENABLED}.
	 */
	public static final String SIGNING_ENABLED = "aws.signing.enabled";

	/**
	 * The AWS region.
	 * <p>
	 * Expects a String value such as {@code us-east-1}.
	 * <p>
	 * No default: must be provided when signing is enabled.
	 */
	public static final String REGION = "aws.region";

	/**
	 * The type of credentials to use when signing is {@link #SIGNING_ENABLED enabled}.
	 * <p>
	 * Expects one of the names listed as constants in {@link ElasticsearchAwsCredentialsTypeNames}.
	 * <p>
	 * Defaults to {@link Defaults#CREDENTIALS_TYPE}.
	 */
	public static final String CREDENTIALS_TYPE = "aws.credentials.type";

	/**
	 * The AWS access key ID when using {@link ElasticsearchAwsCredentialsTypeNames#STATIC static credentials}.
	 * <p>
	 * Expects a String value such as {@code AKIDEXAMPLE}.
	 * <p>
	 * No default: must be provided when signing is enabled and
	 * the credentials type is set to {@link ElasticsearchAwsCredentialsTypeNames#STATIC}.
	 */
	public static final String CREDENTIALS_ACCESS_KEY_ID = "aws.credentials.access_key_id";

	/**
	 * The AWS secret access key when using {@link ElasticsearchAwsCredentialsTypeNames#STATIC static credentials}.
	 * <p>
	 * Expects a String value such as {@code wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY}
	 * <p>
	 * No default: must be provided when signing is enabled and
	 * the credentials type is set to {@link ElasticsearchAwsCredentialsTypeNames#STATIC}.
	 */
	public static final String CREDENTIALS_SECRET_ACCESS_KEY = "aws.credentials.secret_access_key";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final boolean SIGNING_ENABLED = false;
		public static final String CREDENTIALS_TYPE = ElasticsearchAwsCredentialsTypeNames.DEFAULT;
	}
}
