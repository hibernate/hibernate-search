/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.logging.impl;

import static org.hibernate.search.backend.lucene.logging.impl.LuceneLog.ID_OFFSET_LEGACY_ENGINE;
import static org.jboss.logging.Logger.Level.TRACE;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = LuceneInfoStreamLog.CATEGORY_NAME,
		description = """
				Logs the Lucene infostream.
				+
				To enable the logger, the category needs to be enabled at TRACE level and configuration
				property `io.writer.infostream` needs to be enabled on the index.
				+
				See `org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings.IO_WRITER_INFOSTREAM` for more details.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface LuceneInfoStreamLog extends BasicLogger {
	/**
	 * This is the category of the Logger used to print out the Lucene infostream.
	 * <p>
	 * To enable the logger, the category needs to be enabled at TRACE level and configuration
	 * property {@code org.hibernate.search.backend.configuration.impl.IndexWriterSetting#INFOSTREAM}
	 * needs to be enabled on the index.
	 *
	 * @see org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings#IO_WRITER_INFOSTREAM
	 */
	String CATEGORY_NAME = "org.hibernate.search.backend.lucene.infostream";

	LuceneInfoStreamLog INSTANCE =
			LoggerFactory.make( LuceneInfoStreamLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 226, value = "%s: %s")
	void logInfoStreamMessage(String componentName, String message);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
}
