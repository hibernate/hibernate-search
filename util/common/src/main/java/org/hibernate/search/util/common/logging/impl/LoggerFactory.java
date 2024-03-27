/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.logging.impl;

import java.lang.invoke.MethodHandles.Lookup;

import org.jboss.logging.Logger;

public final class LoggerFactory {

	private LoggerFactory() {
		//not allowed
	}

	public static <T> T make(Class<T> logClass, Lookup creationContext) {
		final String className = creationContext.lookupClass().getName();
		return Logger.getMessageLogger( logClass, className );
	}

	public static <T> T make(Class<T> logClass, LogCategory category) {
		return Logger.getMessageLogger( logClass, category.getName() );
	}

}
