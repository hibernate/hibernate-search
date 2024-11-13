/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;

import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET;
import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET_LEGACY_ES;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.ElasticsearchDistributionName;
import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = VersionLog.CATEGORY_NAME,
		description = """
				Logs the warning about an unknown Elasticsearch/OpenSearch version.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface VersionLog {
	String CATEGORY_NAME = "org.hibernate.search.version";

	VersionLog INSTANCE = LoggerFactory.make( VersionLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	@Message(id = ID_OFFSET_LEGACY_ES + 80,
			value = "Unable to detect the Elasticsearch version running on the cluster: %s")
	SearchException failedToDetectElasticsearchVersion(String causeMessage, @Cause Exception e);

	@Message(id = ID_OFFSET_LEGACY_ES + 81,
			value = "Incompatible Elasticsearch version: '%s'."
					+ " Refer to the documentation to know which versions of Elasticsearch"
					+ " are compatible with Hibernate Search.")
	SearchException unsupportedElasticsearchVersion(ElasticsearchVersion version);


	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET_LEGACY_ES + 85,
			value = "Unknown Elasticsearch version running on the cluster: '%s'."
					+ " Hibernate Search may not work correctly."
					+ " Consider updating to a newer version of Hibernate Search, if any.")
	void unknownElasticsearchVersion(ElasticsearchVersion version);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	@Message(id = ID_OFFSET + 56, value = "Invalid Elasticsearch version: '%1$s'."
			+ " Expected format is 'x.y.z-qualifier', where 'x', 'y' and 'z' are integers,"
			+ " and 'qualifier' is an string of word characters (alphanumeric or '_')."
			+ " Incomplete versions are allowed, for example '7.0' or just '7'.")
	SearchException invalidElasticsearchVersionWithoutDistribution(String invalidRepresentation,
			@Cause Throwable cause);

	@Message(id = ID_OFFSET + 57, value = "Invalid Elasticsearch version: '%1$s'."
			+ " Expected format is 'x.y.z-qualifier' or '<distribution>:x.y.z-qualifier' or just '<distribution>',"
			+ " where '<distribution>' is one of %2$s (defaults to '%3$s'),"
			+ " 'x', 'y' and 'z' are integers,"
			+ " and 'qualifier' is an string of word characters (alphanumeric or '_')."
			+ " Incomplete versions are allowed, for example 'elastic:7.0', '7.0' or just '7'."
			+ " Note that the format '<distribution>' without a version number"
			+ " is only useful for distributions that don't support version numbers,"
			+ " such as Amazon OpenSearch Serverless.")
	SearchException invalidElasticsearchVersionWithOptionalDistribution(String invalidRepresentation,
			List<String> validDistributions, String defaultDistribution, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 59, value = "Unexpected Elasticsearch version running on the cluster: '%2$s'."
			+ " Hibernate Search was configured for Elasticsearch '%1$s'.")
	SearchException unexpectedElasticsearchVersion(ElasticsearchVersion configuredVersion,
			ElasticsearchVersion actualVersion);

	@Message(id = ID_OFFSET + 97,
			value = "Missing or imprecise Elasticsearch version:"
					+ " configuration property '%1$s' is set to 'false',"
					+ " so you must set the version explicitly with at least as much precision as 'x.y',"
					+ " where 'x' and 'y' are integers.")
	SearchException impreciseElasticsearchVersionWhenVersionCheckDisabled(String versionCheckPropertyKey);

	@Message(id = ID_OFFSET + 141,
			value = "Incompatible Elasticsearch version:"
					+ " version '%2$s' does not match version '%1$s' that was provided"
					+ " when the backend was created."
					+ " You can provide a more precise version on startup,"
					+ " but you cannot override the version that was provided when the backend was created.")
	SearchException incompatibleElasticsearchVersionOnStart(ElasticsearchVersion versionOnCreation,
			ElasticsearchVersion versionOnStart);

	@Message(id = ID_OFFSET + 173, value = "The targeted Elasticsearch cluster is reachable, but does not expose its version."
			+ " Check that the configured Elasticsearch hosts/URI points to the right server."
			+ " If you are targeting Amazon OpenSearch Serverless, you must set the configuration property '%1$s' explicitly to '%2$s'."
			+ " See the reference documentation for more information.")
	SearchException unableToFetchElasticsearchVersion(String versionConfigPropertyKey,
			ElasticsearchVersion expectedAWSOpenSearchServerlessVersion);

	@Message(id = ID_OFFSET + 174,
			value = "Cannot check the Elasticsearch version because the targeted Elasticsearch distribution '%s' does not expose its version.")
	SearchException cannotCheckElasticsearchVersion(ElasticsearchDistributionName distributionName);

	@Message(id = ID_OFFSET + 175,
			value = "Unexpected Amazon OpenSearch Serverless version: '%1$s'."
					+ " Amazon OpenSearch Serverless doesn't use version numbers."
					+ " Set the version to simply '%2$s'.")
	SearchException unexpectedAwsOpenSearchServerlessVersion(ElasticsearchVersion configuredVersion,
			ElasticsearchVersion expectedAWSOpenSearchServerlessVersion);

	@Message(id = ID_OFFSET + 185,
			value = "The targeted %1$s version is not compatible with the Hibernate Search integration of vector search. "
					+ "To get vector search integration, upgrade your %1$s cluster to version %2$s or later,"
					+ " and if you configured the %1$s version in Hibernate Search, update it accordingly.")
	SearchException searchBackendVersionIncompatibleWithVectorIntegration(String distribution, String version);

}
