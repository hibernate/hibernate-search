/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.testsupport.setup;

import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * A test only {@link org.hibernate.search.exception.ErrorHandler} that maintains counts
 * per exception thrown
 *
 * @author gustavonalle
 */
public class CountingErrorHandler implements ErrorHandler {

	private Map<Class<? extends Throwable>, Integer> stats = new HashMap<Class<? extends Throwable>, Integer>();

	@Override
	public void handle(ErrorContext context) {
		register( context.getThrowable() );
	}
	@Override
	public void handleException(String errorMsg, Throwable exception) {
		register( exception );
	}

	public int getCountFor(Class<? extends Throwable> throwable) {
		Integer count = stats.get( throwable );
		return count == null ? 0 : count;
	}

	private synchronized void register(Throwable exception) {
		Integer count = stats.get( exception.getClass() );
		if ( count == null ) {
			stats.put( exception.getClass(), 1 );
		}
		else {
			stats.put( exception.getClass(), ++ count );
		}
	}
}
