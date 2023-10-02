/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.jberet.logging.impl;

import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.JAKARTA_BATCH_JBERET_ID_RANGE_MIN,
				max = MessageConstants.JAKARTA_BATCH_JBERET_ID_RANGE_MAX),
		// Exceptions for legacy messages from Search 5 (Jakarta Batch Core module)
		@ValidIdRange(min = MessageConstants.JAKARTA_BATCH_CORE_ID_RANGE_MIN,
				max = MessageConstants.JAKARTA_BATCH_CORE_ID_RANGE_MIN + 5),
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5 (Jakarta Batch Core module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_LEGACY = MessageConstants.JAKARTA_BATCH_CORE_ID_RANGE_MIN;

	@Message(id = ID_OFFSET_LEGACY + 2,
			value = "No entity manager factory available in the CDI context with this bean name: '%1$s'."
					+ " Make sure your entity manager factory is a named bean."
	)
	SearchException noAvailableEntityManagerFactoryInCDI(String reference);

	@Message(id = ID_OFFSET_LEGACY + 3,
			value = "Unknown entity manager factory namespace: '%1$s'. Use a supported namespace.")
	SearchException unknownEntityManagerFactoryNamespace(String namespace);

	@Message(id = ID_OFFSET_LEGACY + 4,
			value = "Exception while retrieving the EntityManagerFactory using @PersistenceUnit."
					+ " This generally happens either because persistence wasn't configured properly"
					+ " or because there are multiple persistence units."
	)
	SearchException cannotRetrieveEntityManagerFactoryInJakartaBatch();

	@Message(id = ID_OFFSET_LEGACY + 5,
			value = "Multiple entity manager factories have been registered in the CDI context."
					+ " Use the '" + MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE + "' parameter"
					+ " to provide the bean name for the selected entity manager factory to the mass indexing job."
	)
	SearchException ambiguousEntityManagerFactoryInJakartaBatch();

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET = MessageConstants.JAKARTA_BATCH_JBERET_ID_RANGE_MIN;

}
