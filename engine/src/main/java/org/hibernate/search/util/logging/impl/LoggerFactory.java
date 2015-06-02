/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.logging.impl;

import org.jboss.logging.Logger;

/**
 * A factory class for class loggers. Allows a creation of loggers after the DRY principle.
 *
 * @author Hardy Ferentschik
 */
public final class LoggerFactory {

	private LoggerFactory() {
		//now allowed
	}

	public static Log make() {
		Throwable t = new Throwable();
		StackTraceElement directCaller = t.getStackTrace()[1];
		return Logger.getMessageLogger( Log.class, directCaller.getClassName() );
	}

	public static <T> T make(Class<T> logClass) {
		Throwable t = new Throwable();
		StackTraceElement directCaller = t.getStackTrace()[1];
		return Logger.getMessageLogger( logClass, directCaller.getClassName() );
	}

	public static Log make(LogCategory category) {
		return Logger.getMessageLogger( Log.class, category.getName() );
	}
}
