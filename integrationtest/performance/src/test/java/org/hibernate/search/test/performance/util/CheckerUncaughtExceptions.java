/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.util;

import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.hibernate.search.test.performance.scenario.TestContext;

/**
 * @author Tomas Hradec
 */
public class CheckerUncaughtExceptions {

	private static final List<Throwable> UNCAUGHT_EXCEPTIONS = new CopyOnWriteArrayList<Throwable>();

	private CheckerUncaughtExceptions() {
	}

	public static void initUncaughtExceptionHandler() {
		Thread.setDefaultUncaughtExceptionHandler( new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				UNCAUGHT_EXCEPTIONS.add( e );
			}
		} );
	}

	public static void printUncaughtExceptions(TestContext ctx, PrintWriter outWriter) {
		if ( UNCAUGHT_EXCEPTIONS.size() > 0 ) {
			outWriter.println( "===========================================================================" );
			outWriter.println( "EXCEPTIONS" );
			outWriter.println( "" );
			for ( Throwable e : UNCAUGHT_EXCEPTIONS ) {
				e.printStackTrace( outWriter );
				outWriter.println( "---------------------------------------------------------------" );
			}
		}
	}

}
