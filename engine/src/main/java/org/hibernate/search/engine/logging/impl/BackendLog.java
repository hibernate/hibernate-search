/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.logging.impl;

import static org.hibernate.search.engine.logging.impl.EngineLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = BackendLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface BackendLog {
	String CATEGORY_NAME = "org.hibernate.search.backend";

	BackendLog INSTANCE = LoggerFactory.make( BackendLog.class, CATEGORY_NAME, MethodHandles.lookup() );

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
}
