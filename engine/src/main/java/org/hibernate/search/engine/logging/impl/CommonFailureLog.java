/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.logging.impl;

import static org.hibernate.search.engine.logging.impl.EngineLog.ID_OFFSET;
import static org.jboss.logging.Logger.Level.ERROR;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.EventContextFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Suppressed;

@CategorizedLogger(
		category = CommonFailureLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface CommonFailureLog {
	String CATEGORY_NAME = "org.hibernate.search.common.failures";

	CommonFailureLog INSTANCE = LoggerFactory.make( CommonFailureLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = ID_OFFSET + 19,
			value = "Hibernate Search encountered %3$s failures during %1$s."
					+ " Only the first %2$s failures are displayed here."
					+ " See the TRACE logs for extra failures.")
	String collectedFailureLimitReached(String process, int failureLimit, int failureCount);

	@Message(id = ID_OFFSET + 20,
			value = "Hibernate Search encountered failures during %1$s."
					+ " Failures:\n%2$s")
	SearchException collectedFailures(String process, String renderedFailures,
			@Suppressed Collection<Throwable> failures);

	@LogMessage(level = Logger.Level.TRACE)
	@Message(id = ID_OFFSET + 21,
			value = "Hibernate Search encountered a failure during %1$s;"
					+ " continuing for now to list all problems,"
					+ " but the process will ultimately be aborted.\n"
					+ "%2$s\n" // Context
					+ "Failure:" // The stack trace follows
	)
	void newCollectedFailure(String process,
			@FormatWith(EventContextFormatter.class) EventContextProvider contextProvider,
			@Cause Throwable failure);

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET + 22, value = "Exception while collecting a failure"
			+ " -- this may indicate a bug or a missing test in Hibernate Search."
			+ " Please report it: https://hibernate.org/community/"
			+ " Nested exception: %1$s")
	void exceptionWhileCollectingFailure(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 25,
			value = "Invalid call of ifSupported(...) after orElse(...)."
					+ " Use a separate extension() context, or move the orElse(...) call last."
	)
	SearchException cannotCallDslExtensionIfSupportedAfterOrElse();

	@Message(id = ID_OFFSET + 26,
			value = "None of the provided extensions can be applied to the current context. "
					+ " Attempted extensions: %1$s."
					+ " If you want to ignore this, use .extension().ifSupported(...).orElse(ignored -> { })."
	)
	SearchException dslExtensionNoMatch(List<?> attemptedExtensions);

	@LogMessage(level = Logger.Level.ERROR)
	@Message(id = ID_OFFSET + 69,
			value = "The background failure handler threw an exception while handling a previous failure."
					+ " The failure may not have been reported.")
	void failureInFailureHandler(@Cause Throwable t);

	@Message(id = ID_OFFSET + 112,
			value = "Unable to close saved value for key %1$s: %2$s")
	SearchException unableToCloseSavedValue(String keyName, String message, @Cause Exception e);

	@Message(id = ID_OFFSET + 113,
			value = "Unable to access the Search integration: initialization hasn't completed yet.")
	SearchException noIntegrationBecauseInitializationNotComplete();

	@LogMessage(level = ERROR)
	@Message(id = ID_OFFSET + 128,
			value = "Work discarded, thread was interrupted while waiting for space to schedule: %1$s")
	void interruptedWorkError(Runnable r);

	@LogMessage(level = ERROR)
	@Message(id = ID_OFFSET + 129, value = "%1$s")
	void exceptionOccurred(String errorMsg, @Cause Throwable exceptionThatOccurred);

}
