/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.elasticsearch.aws.logging.impl;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.BACKEND_ES_AWS_ID_RANGE_MIN, max = MessageConstants.BACKEND_ES_AWS_ID_RANGE_MAX)
})
public interface Log extends BasicLogger {

	int ID_OFFSET = MessageConstants.BACKEND_ES_AWS_ID_RANGE_MIN;

	@Message(id = ID_OFFSET + 0,
			value = "AWS request signing is enabled, but mandatory property '%1$s' is not set"
	)
	SearchException missingPropertyForSigning(String propertyKey);

}
