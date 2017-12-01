/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.processing.impl;

import java.util.function.Supplier;

import org.hibernate.search.util.SearchException;

/**
 * @author Yoann Rodiere
 */
public class ProvidedStringIdentifierMapping implements IdentifierMapping<String, Object> {

	private static final ProvidedStringIdentifierMapping INSTANCE = new ProvidedStringIdentifierMapping();

	@SuppressWarnings("unchecked") // This class is bivariant in E
	public static <E> IdentifierMapping<String, E> get() {
		return (IdentifierMapping<String, E>) INSTANCE;
	}

	@Override
	public String getIdentifier(Object providedId, Supplier<?> entityProvider) {
		if ( providedId == null ) {
			throw new SearchException( "The identifier for this entity should always be provided,"
					+ " but the provided identifier was null." );
		}
		return String.valueOf( providedId );
	}

	@Override
	public String toDocumentIdentifier(String identifier) {
		return identifier;
	}

	@Override
	public String fromDocumentIdentifier(String documentId) {
		return documentId;
	}

}
