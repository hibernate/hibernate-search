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
package org.hibernate.search.engine.service.classloading.spi;

import org.hibernate.search.exception.SearchException;

/**
 * Indicates a problem performing class loading.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class ClassLoadingException extends SearchException {
	/**
	 * Constructs a {@code ClassLoadingException} using the specified message and cause.
	 *
	 * @param message A message explaining the exception condition.
	 * @param cause The underlying cause
	 */
	public ClassLoadingException(String message, Throwable cause) {
		super( message, cause );
	}

	/**
	 * Constructs a {@code ClassLoadingException} using the specified message.
	 *
	 * @param message A message explaining the exception condition.
	 */
	public ClassLoadingException(String message) {
		super( message );
	}
}


