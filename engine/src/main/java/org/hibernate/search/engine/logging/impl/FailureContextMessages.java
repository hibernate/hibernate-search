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
 * Message bundle for failure contexts.
 */
@MessageBundle(projectCode = "HSEARCH")
public interface FailureContextMessages {

	@Message(value = ", ")
	String contextSeparator();

	@Message(value = "    ")
	String contextIndent();

	@Message(value = ": ")
	String contextFailuresSeparator();

	@Message(value = "  - ")
	String contextFailuresBulletPoint();

	/**
	 * @return A message with the same length as {@link #contextFailuresBulletPoint()}, but containing only blanks.
	 */
	@Message(value = "    ")
	String contextFailuresNoBulletPoint();

	@Message(value = "Context: ")
	String contextPrefix();

	@Message(value = "failures")
	String failures();

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
