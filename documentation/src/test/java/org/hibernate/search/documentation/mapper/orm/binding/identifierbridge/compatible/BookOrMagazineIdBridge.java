/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.compatible;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

//tag::include[]
public class BookOrMagazineIdBridge implements IdentifierBridge<BookOrMagazineId> {

	@Override
	public String toDocumentIdentifier(BookOrMagazineId value,
			IdentifierBridgeToDocumentIdentifierContext context) {
		return value.getPublisherId() + "/" + value.getPublisherSpecificBookId();
	}

	@Override
	public BookOrMagazineId fromDocumentIdentifier(String documentIdentifier,
			IdentifierBridgeFromDocumentIdentifierContext context) {
		String[] split = documentIdentifier.split( "/" );
		return new BookOrMagazineId( Long.parseLong( split[0] ), Long.parseLong( split[1] ) );
	}

	@Override
	public boolean isCompatibleWith(IdentifierBridge<?> other) {
		return getClass().equals( other.getClass() ); // <1>
	}
}
//end::include[]
