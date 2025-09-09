/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.backend.elasticsearch.aws.logging.impl;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.TRACE;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.regions.Region;

@CategorizedLogger(
		category = AwsLog.CATEGORY_NAME,
		description = """
				Logs related to the AWS integration of the Elasticsearch backend.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.BACKEND_ES_AWS_ID_RANGE_MIN,
				max = MessageConstants.BACKEND_ES_AWS_ID_RANGE_MAX)
})
public interface AwsLog extends BasicLogger {

	String CATEGORY_NAME = "org.hibernate.search.elasticsearch.aws";

	AwsLog INSTANCE = LoggerFactory.make( AwsLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	int ID_OFFSET = MessageConstants.BACKEND_ES_AWS_ID_RANGE_MIN;

	@Message(id = ID_OFFSET,
			value = "When AWS request signing is enabled, this property must be set."
	)
	SearchException missingPropertyForSigning();

	@Message(id = ID_OFFSET + 1,
			value = "When AWS request signing is enabled with credentials of type '%1$s', this property must be set."
	)
	SearchException missingPropertyForSigningWithCredentialsType(String credentialsType);

	@Message(id = ID_OFFSET + 2,
			value = "Invalid credentials configuration for AWS request signing."
					+ " The configuration properties '%1$s' and ' '%2$s' are now obsolete."
					+ " In order to specify static credentials, set property '%3$s' to '%4$s',"
					+ " then set the access key ID using property '%5$s'"
					+ " and the secret access key using property '%6$s'."
	)
	SearchException obsoleteAccessKeyIdOrSecretAccessKeyForSigning(String legacyAccessKeyIdPropertyKey,
			String legacySecretAccessKeyPropertyKey,
			String credentialsTypePropertyKey, String credentialsTypePropertyValueStatic,
			String accessKeyIdPropertyKey, String secretAccessKeyPropertyKey);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 3, value = "HTTP request (before signing): %s")
	void httpRequestBeforeSigning(Object request);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 4, value = "AWS request (before signing): %s")
	void awsRequestBeforeSigning(SdkHttpFullRequest awsRequest);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 5, value = "AWS credentials: %s")
	void awsCredentials(AwsCredentials credentials);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 6, value = "AWS request (after signing): %s")
	void httpRequestAfterSigning(SignedRequest signedRequest);

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 7, value = "HTTP request (after signing): %s")
	void awsRequestAfterSigning(Object request);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 8, value = "AWS request signing is disabled.")
	void signingDisabled();

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 9,
			value = "AWS request signing is enabled [region = '%s', service = '%s', credentialsProvider = '%s'].")
	void signingEnabled(Region region, String service, AwsCredentialsProvider credentialsProvider);
}
