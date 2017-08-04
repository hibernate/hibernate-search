/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import org.hibernate.search.util.SearchException;

/**
 * @author Yoann Rodiere
 */
public class ProvidedToStringIdentifierConverter implements IdentifierConverter<String, Object> {

	private static final ProvidedToStringIdentifierConverter INSTANCE = new ProvidedToStringIdentifierConverter();

	@SuppressWarnings("unchecked") // This class is bivariant in E
	public static <E> IdentifierConverter<String, E> get() {
		return (IdentifierConverter<String, E>) INSTANCE;
	}

	@Override
	public String toDocumentId(Object providedId, Object entity) {
		if ( providedId == null ) {
			throw new SearchException( "The identifier for this entity should always be provided,"
					+ " but the provided identifier was null." );
		}
		return String.valueOf( providedId );
	}

	@Override
	public String fromDocumentId(String id) {
		return id;
	}

}
