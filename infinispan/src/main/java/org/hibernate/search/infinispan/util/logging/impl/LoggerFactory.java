/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan.util.logging.impl;

import org.hibernate.search.infinispan.logging.impl.Log;
import org.jboss.logging.Logger;

/**
 * Factory for obtaining {@link Logger} instances.
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2012 Red Hat Inc.
 * @author Gunnar Morling
 */
public class LoggerFactory {

	private static final CallerProvider callerProvider = new CallerProvider();

	private LoggerFactory() {
	}

	public static Log make() {
		return Logger.getMessageLogger( Log.class, callerProvider.getCallerClass().getCanonicalName() );
	}

	private static class CallerProvider extends SecurityManager {

		public Class<?> getCallerClass() {
			return getClassContext()[2];
		}
	}
}
