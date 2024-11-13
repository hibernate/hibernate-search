/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.logging.impl;

import static org.hibernate.search.engine.logging.impl.EngineLog.ID_OFFSET;
import static org.jboss.logging.Logger.Level.DEBUG;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.Set;

import org.hibernate.search.engine.cfg.spi.ConfigurationProvider;
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
		category = ConfigurationLog.CATEGORY_NAME,
		description = """
				Logs warnings about unused configuration properties and tracking of such properties.
				+
				Debug logs may also include information on configuration providers or other configuration-related issues.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface ConfigurationLog {
	String CATEGORY_NAME = "org.hibernate.search.configuration";

	ConfigurationLog INSTANCE = LoggerFactory.make( ConfigurationLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = ID_OFFSET + 1,
			value = "Invalid value for configuration property '%1$s': '%2$s'. %3$s")
	SearchException unableToConvertConfigurationProperty(String key, Object rawValue, String errorMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 66,
			value = "Invalid configuration property checking strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidConfigurationPropertyCheckingStrategyName(String invalidRepresentation,
			List<String> validRepresentations);

	@LogMessage(level = Logger.Level.INFO)
	@Message(id = ID_OFFSET + 67,
			value = "Configuration property tracking is disabled; unused properties will not be logged.")
	void configurationPropertyTrackingDisabled();

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET + 68,
			value = "Invalid configuration passed to Hibernate Search: some properties in the given configuration are not used."
					+ " There might be misspelled property keys in your configuration."
					+ " Unused properties: %1$s."
					+ " To disable this warning, set the property '%2$s' to '%3$s'.")
	void configurationPropertyTrackingUnusedProperties(Set<String> propertyKeys, String disableWarningKey,
			String disableWarningValue);

	@Message(id = ID_OFFSET + 73,
			value = "Invalid configuration passed to Hibernate Search: some properties in the given configuration are obsolete."
					+ "Configuration properties changed between Hibernate Search 5 and Hibernate Search 6"
					+ " Check out the reference documentation and upgrade your configuration."
					+ " Obsolete properties: %1$s.")
	SearchException obsoleteConfigurationPropertiesFromSearch5(Set<String> propertyKeys);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 116,
			value = "Ignoring ServiceConfigurationError caught while trying to instantiate service '%s'.")
	void ignoringServiceConfigurationError(Class<?> serviceContract, @Cause ServiceConfigurationError error);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 120,
			value = "Multiple configuration providers are available for scope '%1$s'. "
					+ "They will be taken under consideration in the following order: '%2$s'.")
	void multipleConfigurationProvidersAvailable(String scope, List<ConfigurationProvider> configurationProviders);

}
