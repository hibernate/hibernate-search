/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
