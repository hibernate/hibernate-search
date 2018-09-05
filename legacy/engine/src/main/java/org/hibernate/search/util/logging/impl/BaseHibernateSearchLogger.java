/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.logging.impl;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Parent interface for all Loggers of the various compilations units
 * of Hibernate Search.
 * Sharing the same messages across all units should be avoided but
 * we don't want to strictly enforce this rule so to maintain historical
 * stability of logging messages defined before this split, or to allow
 * the occasional reasonable exception.
 *
 * The primary reason to avoid this is to encourage each module to define
 * more specific error messages and codes, but also to consider is that
 * blindly inheriting all messages from another module causes the Logger
 * annotation processor to generate a significant amount of code, which
 * is mostly unnecessary as most of those codes are unlikely to be actually
 * reused by other modules.
 *
 * @author Sanne Grinovero
 */
@MessageLogger(projectCode = "HSEARCH")
public interface BaseHibernateSearchLogger extends BasicLogger {

	/*
	 * Below we have messages moved from the hibernate-search-engine specific
	 * module to allow reuse from other modules, but their id will need to respect
	 * the original one. Always assign a module in the original component first
	 * before promoting the message.
	 * Ideally avoid promoting any message and avoid reusing across modules.
	 */

	@LogMessage(level = WARN)
	@Message(id = 49,
			value = "'%s' was interrupted while waiting for index activity to finish. Index might be inconsistent or have a stale lock")
	void interruptedWhileWaitingForIndexActivity(String name, @Cause InterruptedException e);

	@LogMessage(level = ERROR)
	@Message(id = 69, value = "Illegal object retrieved from message")
	void illegalObjectRetrievedFromMessage(@Cause Exception e);

	@Message(id = 114, value = "Could not load resource: '%1$s'")
	SearchException unableToLoadResource(String fileName);

	@Message(id = 140, value = "Unknown Resolution: %1$s")
	AssertionFailure unknownResolution(String resolution);

	@Message(id = 266, value = "'%s' is not a valid type for a facet range request. Numbers (byte, short, int, long, float, double and their wrappers) as well as dates are supported")
	SearchException unsupportedFacetRangeParameter(String type);

	@Message(id = 324, value = "The fieldBridge for field '%1$s' is an instance of '%2$s', which does not implement TwoWayFieldBridge. Projected fields must have a TwoWayFieldBridge.")
	SearchException projectingFieldWithoutTwoWayFieldBridge(String fieldName, Class<?> fieldBridgeClass);

	@Message(id = 327, value = "Unsupported indexNullAs token type '%3$s' on field '%2$s' of entity '%1$s'." )
	SearchException unsupportedNullTokenType(@FormatWith(IndexedTypeIdentifierFormatter.class) IndexedTypeIdentifier entityName, String fieldName, Class<?> tokenType);

	@Message(id = 329, value = "Property '" + Environment.ANALYSIS_DEFINITION_PROVIDER + "' set to value '%1$s' is invalid."
			+ " The value must be the fully-qualified name of a class with a public, no-arg constructor in your classpath."
			+ " Also, the class must either implement LuceneAnalyzerDefinitionProvider or expose a public,"
			+ " @Factory-annotated method returning a LuceneAnalyzerDefinitionProvider.")
	SearchException invalidLuceneAnalyzerDefinitionProvider(String providerClassName, @Cause Exception e);

}
