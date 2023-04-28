/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.simple;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

//tag::include[]
public class BookIdBridge implements IdentifierBridge<BookId> { // <1>

	@Override
	public String toDocumentIdentifier(BookId value,
			IdentifierBridgeToDocumentIdentifierContext context) { // <2>
		return value.getPublisherId() + "/" + value.getPublisherSpecificBookId();
	}

	@Override
	public BookId fromDocumentIdentifier(String documentIdentifier,
			IdentifierBridgeFromDocumentIdentifierContext context) { // <3>
		String[] split = documentIdentifier.split( "/" );
		return new BookId( Long.parseLong( split[0] ), Long.parseLong( split[1] ) );
	}

}
//end::include[]
