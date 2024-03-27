/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.reporting.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Message bundle for event contexts related to engine concepts.
 */
@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface EngineEventContextMessages {

	EngineEventContextMessages INSTANCE = Messages.getBundle( EngineEventContextMessages.class );

	@Message(value = "bootstrap")
	String bootstrap();

	@Message(value = "shutdown")
	String shutdown();

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

	@Message(value = "default backend")
	String defaultBackend();

	@Message(value = "backend '%1$s'")
	String backend(String name);

	@Message(value = "index '%1$s'")
	String index(String name);

	@Message(value = "indexes %1$s")
	String indexes(Set<String> names);

	@Message(value = "shard '%1$s'")
	String shard(String shardId);

	@Message(value = "index schema root")
	String indexSchemaRoot();

	@Message(value = "identifier")
	String indexSchemaIdentifier();

	@Message(value = "field '%1$s'")
	String indexFieldAbsolutePath(String absolutePath);

	@Message(value = "fields %1$s")
	String indexFieldAbsolutePaths(List<String> absolutePaths);

	@Message(value = "field template '%1$s'")
	String fieldTemplate(String absolutePath);

	@Message(value = "analyzer '%1$s'")
	String analyzer(String name);

	@Message(value = "normalizer '%1$s'")
	String normalizer(String name);

	@Message(value = "char filter '%1$s'")
	String charFilter(String name);

	@Message(value = "tokenizer '%1$s'")
	String tokenizer(String name);

	@Message(value = "token filter '%1$s'")
	String tokenFilter(String name);

}
