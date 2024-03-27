/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.reporting.impl;

import org.hibernate.search.engine.backend.reporting.spi.BackendSearchHints;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface LuceneSearchHints extends BackendSearchHints {

	LuceneSearchHints INSTANCE = Messages.getBundle( LuceneSearchHints.class );

	@Message("A document projection represents a root document and adding it as a part of the nested object projection might produce misleading results.")
	String documentProjectionNestingNotSupportedHint();
}
