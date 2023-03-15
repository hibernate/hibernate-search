/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.reporting.impl;

import org.hibernate.search.engine.backend.reporting.spi.BackendHints;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface LuceneBackendHints extends BackendHints {

	LuceneBackendHints INSTANCE = Messages.getBundle( LuceneBackendHints.class );

	@Message(value = "Only stored fields can be highlighted. Mark this field as projectable to make highlighting work for it.")
	String highlightNotSupportedAdditionalMessage();

}
