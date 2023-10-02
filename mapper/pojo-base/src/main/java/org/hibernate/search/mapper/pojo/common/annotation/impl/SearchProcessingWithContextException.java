/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
