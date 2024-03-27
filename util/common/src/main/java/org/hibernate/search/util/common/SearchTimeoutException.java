/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common;

import org.hibernate.search.util.common.annotation.impl.SuppressForbiddenApis;

/**
 * Represent a timeout during a Hibernate Search operation.
 */
public class SearchTimeoutException extends SearchException {
	@SuppressForbiddenApis(reason = SEARCH_EXCEPTION_AND_SUBCLASSES_CAN_USE_CONSTRUCTOR)
	public SearchTimeoutException(String message, Throwable cause) {
		super( message, cause );
	}

	@SuppressForbiddenApis(reason = SEARCH_EXCEPTION_AND_SUBCLASSES_CAN_USE_CONSTRUCTOR)
	public SearchTimeoutException(String message) {
		super( message );
	}

}

