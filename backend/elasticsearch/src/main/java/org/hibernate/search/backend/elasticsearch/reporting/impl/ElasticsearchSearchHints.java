/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.reporting.impl;

import org.hibernate.search.engine.backend.reporting.spi.BackendSearchHints;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface ElasticsearchSearchHints extends BackendSearchHints {

	ElasticsearchSearchHints INSTANCE = Messages.getBundle( ElasticsearchSearchHints.class );

	@Message(
			value = "A JSON hit projection represents a root hit object and adding it as a part of the nested object projection might produce misleading results.")
	String jsonHitProjectionNestingNotSupportedHint();

	@Message(
			value = "A source projection represents a root source object and adding it as a part of the nested object projection might produce misleading results.")
	String sourceProjectionNestingNotSupportedHint();
}
