/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.jberet.logging.impl;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		// Search 5 :
		@ValidIdRange(min = MessageConstants.BATCH_JSR352_CORE_ID_RANGE_MIN, max = MessageConstants.BATCH_JSR352_CORE_ID_RANGE_MIN + 5),
		// Search 6 :
		@ValidIdRange(min = MessageConstants.BATCH_JSR352_JBERET_ID_RANGE_MIN, max = MessageConstants.BATCH_JSR352_JBERET_ID_RANGE_MAX)
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5 (JSR-352 Core module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_1 = MessageConstants.BATCH_JSR352_CORE_ID_RANGE_MIN;

	@Message(id = ID_OFFSET_1 + 2,
			value = "No entity manager factory available in the CDI context with this bean name: '%1$s'."
					+ " Make sure your entity manager factory is a named bean."
	)
	SearchException noAvailableEntityManagerFactoryInCDI(String reference);

	@Message(id = ID_OFFSET_1 + 3,
			value = "Unknown entity manager factory namespace: '%1$s'. Please use a supported namespace.")
	SearchException unknownEntityManagerFactoryNamespace(String namespace);

	@Message(id = ID_OFFSET_1 + 4,
			value = "Exception while retrieving the EntityManagerFactory using @PersistenceUnit."
					+ " This generally happens either because the persistence wasn't configured properly"
					+ " or because there are multiple persistence units."
	)
	SearchException cannotRetrieveEntityManagerFactoryInJsr352();

	@Message(id = ID_OFFSET_1 + 5,
			value = "Multiple entity manager factories have been registered in the CDI context."
					+ " Please provide the bean name for the selected entity manager factory to the batch indexing job through"
					+ " the 'entityManagerFactoryReference' parameter."
	)
	SearchException ambiguousEntityManagerFactoryInJsr352();

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET_2 = MessageConstants.BATCH_JSR352_JBERET_ID_RANGE_MIN;

}
