/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.util;

import java.util.Date;
import java.util.Properties;

import org.apache.commons.lang.time.DateFormatUtils;

/**
 * @author Tomas Hradec
 */
public class Util {

	private static final int GC_CYCLE = 3;
	private static final int GC_TIMEOUT = 3 * 1000;

	private Util() {
	}

	public static void runGarbageCollectorAndWait() {
		try {
			for ( int i = 0; i < GC_CYCLE; i++ ) {
				System.gc();
				Thread.sleep( GC_TIMEOUT );
			}
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
	}

	public static void log(String msg) {
		System.out.println( DateFormatUtils.format( new Date(), "[yyyy-MM-dd HH:mm:ss.SSS]" ) + " " + msg );
	}

	public static void setDefaultProperty(Properties properties, String key, String value) {
		if ( !System.getProperties().containsKey( key ) ) {
			properties.setProperty( key, value );
		}
	}

}
