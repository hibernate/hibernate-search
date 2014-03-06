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
package org.hibernate.search.query.hibernate.impl;

import java.util.List;

import org.hibernate.search.util.impl.CollectionHelper;

/**
 * @author Emmanuel Bernard
 */
public class LoaderHelper {
	private static final List<String> objectNotFoundExceptions = CollectionHelper.newArrayList( 2 );

	static {
		objectNotFoundExceptions.add( "org.hibernate.ObjectNotFoundException" );
		objectNotFoundExceptions.add( "javax.persistence.EntityNotFoundException" );
	}

	private LoaderHelper() {
	}

	public static boolean isObjectNotFoundException(RuntimeException e) {
		boolean objectNotFound = false;
		String exceptionClassName = e.getClass().getName();
		for ( String fqc : objectNotFoundExceptions ) {
			if ( fqc.equals( exceptionClassName ) ) {
				objectNotFound = true;
				break;
			}
		}
		return objectNotFound;
	}
}
