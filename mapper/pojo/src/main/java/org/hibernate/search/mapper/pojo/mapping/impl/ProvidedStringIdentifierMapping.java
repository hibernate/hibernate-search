/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.mapping.context.spi.AbstractPojoMappingContextImplementor;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;


public class ProvidedStringIdentifierMapping implements IdentifierMapping<String, Object> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ProvidedStringIdentifierMapping INSTANCE = new ProvidedStringIdentifierMapping();

	@SuppressWarnings("unchecked") // This class is bivariant in E
	public static <E> IdentifierMapping<String, E> get() {
		return (IdentifierMapping<String, E>) INSTANCE;
	}

	@Override
	public String getIdentifier(Object providedId) {
		if ( providedId == null ) {
			throw log.nullProvidedIdentifier();
		}
		return String.valueOf( providedId );
	}

	@Override
	public String getIdentifier(Object providedId, Supplier<?> entityProvider) {
		return getIdentifier( providedId );
	}

	@Override
	public String toDocumentIdentifier(String identifier, AbstractPojoMappingContextImplementor context) {
		return identifier;
	}

	@Override
	public String fromDocumentIdentifier(String documentId, AbstractPojoSessionContextImplementor sessionContext) {
		return documentId;
	}

}
