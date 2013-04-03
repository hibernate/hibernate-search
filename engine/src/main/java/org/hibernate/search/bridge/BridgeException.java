/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.bridge;

import org.hibernate.search.SearchException;

/**
 * Exceptions thrown in a bridge are wrapped in BridgeException
 * The BridgeException message provides useful contextual information
 * to the developers.
 *
 * @author Emmanuel Bernard
 */
public class BridgeException extends SearchException {

	public BridgeException() {
		super();
	}

	public BridgeException(String message) {
		super( message );
	}

	public BridgeException(String message, Throwable cause) {
		super( message, cause );
	}

	public BridgeException(Throwable cause) {
		super( cause );
	}

}
