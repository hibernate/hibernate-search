/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		return Logger.getMessageLogger( creationContext, logClass, className );
	}

	public static <T> T make(Class<T> logClass, LogCategory category, Lookup creationContext) {
		return Logger.getMessageLogger( creationContext, logClass, category.getName() );
	}

}
