/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.logging.impl;

import java.lang.invoke.MethodHandles.Lookup;

import org.jboss.logging.Logger;

/**
 * A factory class for class loggers. Allows a creation of loggers after the DRY principle.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public final class LoggerFactory {

	private LoggerFactory() {
		//now allowed
	}

	public static Log make(Lookup creationContext) {
		return make( Log.class, creationContext );
	}

	public static <T> T make(Class<T> logClass, Lookup creationContext) {
		final String className = creationContext.lookupClass().getName();
		return Logger.getMessageLogger( logClass, className );
	}

	public static Log make(LogCategory category) {
		return make( Log.class, category );
	}

	public static <T> T make(Class<T> logClass, LogCategory category) {
		return Logger.getMessageLogger( logClass, category.getName() );
	}
}
