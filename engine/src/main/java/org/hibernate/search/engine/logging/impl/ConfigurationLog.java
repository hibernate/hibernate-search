/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.logging.impl;

import static org.hibernate.search.engine.logging.impl.EngineLog.ID_OFFSET;
import static org.jboss.logging.Logger.Level.DEBUG;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
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
				Logs related to Hibernate Search configuration in general.
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

	@Message(id = ID_OFFSET + 33,
			value = "No backend with name '%1$s'."
					+ " Check that at least one entity is configured to target that backend."
					+ " The following backends can be retrieved by name: %2$s."
					+ " %3$s")
	SearchException unknownNameForBackend(String backendName, Collection<String> validBackendNames,
			String defaultBackendMessage);

	@Message(id = ID_OFFSET + 34,
			value = "No index manager with name '%1$s'."
					+ " Check that at least one entity is configured to target that index."
					+ " The following indexes can be retrieved by name: %2$s.")
	SearchException unknownNameForIndexManager(String indexManagerName, Collection<String> validIndexNames);

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

	@Message(id = ID_OFFSET + 75,
			value = "No default backend."
					+ " Check that at least one entity is configured to target the default backend."
					+ " The following backends can be retrieved by name: %1$s.")
	SearchException noDefaultBackendRegistered(Collection<String> validBackendNames);

	@Message(id = ID_OFFSET + 81,
			value = "Unable to resolve backend type:"
					+ " configuration property '%1$s' is not set, and there isn't any backend in the classpath."
					+ " Check that you added the desired backend to your project's dependencies.")
	SearchException noBackendFactoryRegistered(String propertyKey);

	@Message(id = ID_OFFSET + 82,
			value = "Ambiguous backend type:"
					+ " configuration property '%1$s' is not set, and multiple backend types are present in the classpath."
					+ " Set property '%1$s' to one of the following to select the backend type: %2$s")
	SearchException multipleBackendFactoriesRegistered(String propertyKey, Collection<String> backendTypeNames);

	@Message(id = ID_OFFSET + 96, value = "Different mappings trying to define two backends " +
			"with the same name '%1$s' but having different expectations on multi-tenancy.")
	SearchException differentMultiTenancyNamedBackend(String backendName);

	@Message(id = ID_OFFSET + 97, value = "Different mappings trying to define default backends " +
			"having different expectations on multi-tenancy.")
	SearchException differentMultiTenancyDefaultBackend();

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
