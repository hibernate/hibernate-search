/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.binder;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

//tag::include[]
public class BookIdBinder implements IdentifierBinder { // <1>

	@Override
	public void bind(IdentifierBindingContext<?> context) { // <2>
		context.bridge( // <3>
				BookId.class, // <4>
				new Bridge() // <5>
		);
	}

	private static class Bridge implements IdentifierBridge<BookId> { // <6>
		@Override
		public String toDocumentIdentifier(BookId value,
				IdentifierBridgeToDocumentIdentifierContext context) {
			return value.getPublisherId() + "/" + value.getPublisherSpecificBookId();
		}

		@Override
		public BookId fromDocumentIdentifier(String documentIdentifier,
				IdentifierBridgeFromDocumentIdentifierContext context) {
			String[] split = documentIdentifier.split( "/" );
			return new BookId( Long.parseLong( split[0] ), Long.parseLong( split[1] ) );
		}
	}
}
//end::include[]
