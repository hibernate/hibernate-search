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
