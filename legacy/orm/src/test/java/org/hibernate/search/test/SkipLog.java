/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test;

import org.hibernate.search.util.logging.impl.Log;
import org.jboss.logging.Logger;

/**
 * Well-known-location lookup for the test-skip log...
 *
 * @author Steve Ebersole
 */
public final class SkipLog {

	public static final Log LOG = Logger.getMessageLogger( Log.class, "org.hibernate.search.test.SKIPPED" );

	private SkipLog() {
		//not allowed
	}

}
