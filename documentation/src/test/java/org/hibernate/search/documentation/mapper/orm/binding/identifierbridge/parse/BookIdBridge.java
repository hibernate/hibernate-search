/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.parse;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

//tag::include[]
public class BookIdBridge implements IdentifierBridge<BookId> { // <1>

	// Implement mandatory toDocumentIdentifier/fromDocumentIdentifier ...
	// ...
	//end::include[]
	@Override
	public String toDocumentIdentifier(BookId value, IdentifierBridgeToDocumentIdentifierContext context) {
		return value.getPublisherId() + "/" + value.getPublisherSpecificBookId();
	}

	@Override
	public BookId fromDocumentIdentifier(String documentIdentifier,
			IdentifierBridgeFromDocumentIdentifierContext context) {
		String[] split = documentIdentifier.split( "/" );
		return new BookId( Long.parseLong( split[0] ), Long.parseLong( split[1] ) );
	}
	//tag::include[]

	@Override
	public BookId parseIdentifierLiteral(String value) { // <2>
		if ( value == null ) {
			return null;
		}
		String[] parts = value.split( "/" );
		if ( parts.length != 2 ) {
			throw new IllegalArgumentException( "BookId string literal must be in a `pubId/bookId` format." );
		}
		return new BookId( Long.parseLong( parts[0] ), Long.parseLong( parts[1] ) );
	}
}
//end::include[]
