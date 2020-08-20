/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
