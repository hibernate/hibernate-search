/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.avro.logging.impl;

import static org.jboss.logging.Logger.Level.WARN;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.BaseHibernateSearchLogger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Hibernate Search Avro Serialization log abstraction.
 *
 * @author Hardy Ferentschik
 */
@MessageLogger(projectCode = "HSEARCH")
public interface Log extends BaseHibernateSearchLogger {

	//Recently moved from the shared module so not adding the message id to the
	//AVRO_SERIALIZATION_MESSAGES_START_ID base offset yet
	@LogMessage(level = Level.DEBUG)
	@Message(id = 79, value = "Serialization protocol version %1$d.%2$d initialized")
	void serializationProtocol(int major, int minor);

	//Recently moved from the shared module so not adding the message id to the
	//AVRO_SERIALIZATION_MESSAGES_START_ID base offset yet
	@Message(id = 98, value = "Unable to parse message from protocol version %1$d.%2$d. "
			+ "Current protocol version: %3$d.%4$d")
	SearchException incompatibleProtocolVersion(int messageMajor, int messageMinor, int currentMajor, int currentMinor);

	@Message(id = AVRO_SERIALIZATION_MESSAGES_START_ID + 1, value = "Unable to find Avro schema '%s'")
	SearchException unableToLoadAvroSchema(String avroSchemaFile);

	@LogMessage(level = WARN)
	@Message(id = AVRO_SERIALIZATION_MESSAGES_START_ID + 2, value = "Parsing message from a future protocol version."
			+ " Some feature might not be propagated. Message version: %1$d.%2$d. Current protocol version: %1$d.%3$d")
	void unexpectedMinorProtocolVersion(int majorVersion, int minorVersion, int latestKnownMinor);

}
