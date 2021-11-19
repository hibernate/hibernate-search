/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.common.annotation.impl;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;
import org.hibernate.search.util.common.reporting.EventContext;

public class SearchProcessingWithContextException extends SearchException {
	@SuppressForbiddenApis(reason = SEARCH_EXCEPTION_AND_SUBCLASSES_CAN_USE_CONSTRUCTOR)
	public SearchProcessingWithContextException(String message, Throwable cause, EventContext context) {
		super( message, cause, context );
	}
}
