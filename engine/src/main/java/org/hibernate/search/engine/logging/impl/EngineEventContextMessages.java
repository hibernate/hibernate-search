/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.logging.impl;

import java.util.Set;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Message bundle for event contexts related to engine concepts.
 */
@MessageBundle(projectCode = "HSEARCH")
public interface EngineEventContextMessages {

	@Message(value = "    ")
	String failureReportContextIndent();

	@Message(value = ": ")
	String failureReportContextFailuresSeparator();

	@Message(value = "  - ")
	String failureReportFailuresBulletPoint();

	/**
	 * @return A message with the same length as {@link #failureReportFailuresBulletPoint()}, but containing only blanks.
	 */
	@Message(value = "    ")
	String failureReportFailuresNoBulletPoint();

	@Message(value = "failures")
	String failureReportFailures();

	/**
	 * @return A string used when a context element is missing.
	 * Should only be used if there is a bug in Hibernate Search.
	 */
	@Message(value = "<DEFAULT>")
	String defaultOnMissingContextElement();

	@Message(value = "type '%1$s'")
	String type(String name);

	@Message(value = "backend '%1$s'")
	String backend(String name);

	@Message(value = "index '%1$s'")
	String index(String name);

	@Message(value = "indexes %1$s")
	String indexes(Set<String> names);

	@Message(value = "index schema root")
	String indexSchemaRoot();

	@Message(value = "field '%1$s'")
	String indexFieldAbsolutePath(String absolutePath);

}
