/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
