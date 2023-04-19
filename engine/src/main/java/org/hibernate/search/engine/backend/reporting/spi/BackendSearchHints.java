/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.reporting.spi;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface BackendSearchHints {

	BackendSearchHints NONE = Messages.getBundle( BackendSearchHints.class );

	@Message("An ID projection represents the document ID and adding it as a part of the nested object projection might produce misleading results "
			+ "since it is always a root document ID and not a nested object ID.")
	String idProjectionNestingNotSupportedHint();

	@Message("A document reference projection represents a root document and adding it as a part of the nested object projection might produce misleading results.")
	String documentReferenceProjectionNestingNotSupportedHint();

	@Message("An entity projection represents a root entity and adding it as a part of the nested object projection might produce misleading results.")
	String entityProjectionNestingNotSupportedHint();

	@Message("An entity reference projection represents a root entity and adding it as a part of the nested object projection might produce misleading results.")
	String entityReferenceProjectionNestingNotSupportedHint();

	@Message("An explanation projection describes the score computation for the hit and adding it as a part of the nested object projection might produce misleading results.")
	String explanationProjectionNestingNotSupportedHint();

	@Message("A score projection provides the score for the entire hit and adding it as a part of the nested object projection might produce misleading results.")
	String scoreProjectionNestingNotSupportedHint();
}
