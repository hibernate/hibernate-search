/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.aws.cfg;

/**
 * AWS-specific configuration properties for Elasticsearch backends.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key;
 * see {@link org.hibernate.search.engine.cfg.BackendSettings} for details.
 */
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
	 * The AWS access key.
	 * <p>
	 * Expects a String value such as {@code AKIDEXAMPLE}.
	 * <p>
	 * No default: must be provided when signing is enabled.
	 */
	public static final String SIGNING_ACCESS_KEY = "aws.signing.access_key";

	/**
	 * The AWS secret key.
	 * <p>
	 * Expects a String value such as {@code wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY}
	 * <p>
	 * No default: must be provided when signing is enabled.
	 */
	public static final String SIGNING_SECRET_KEY = "aws.signing.secret_key";

	/**
	 * The AWS region.
	 * <p>
	 * Expects a String value such as {@code us-east-1}.
	 * <p>
	 * No default: must be provided when signing is enabled.
	 */
	public static final String SIGNING_REGION = "aws.signing.region";

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final boolean SIGNING_ENABLED = false;
	}
}
