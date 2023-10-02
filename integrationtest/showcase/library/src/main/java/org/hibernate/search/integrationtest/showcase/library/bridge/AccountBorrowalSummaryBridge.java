/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.bridge;

import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.integrationtest.showcase.library.model.Account;
import org.hibernate.search.integrationtest.showcase.library.model.Borrowal;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

/**
 * Create a summary of borrowals for a given user:
 * <code><pre>
 * "borrowals": {
 *   "shortTermCount": [integer],
 *   "longTermCount": [integer],
 *   "totalCount": [integer]
 * }
 * </pre></code>
 *
 */
public class AccountBorrowalSummaryBridge implements TypeBridge<Account> {

	private final IndexObjectFieldReference borrowalsObjectFieldReference;
	private final IndexFieldReference<Integer> shortTermBorrowalCountReference;
	private final IndexFieldReference<Integer> longTermBorrowalCountReference;
	private final IndexFieldReference<Integer> totalBorrowalCountReference;

	private AccountBorrowalSummaryBridge(IndexSchemaElement indexSchemaElement) {
		IndexSchemaObjectField borrowalsObjectField = indexSchemaElement.objectField( "borrowals" );
		this.borrowalsObjectFieldReference = borrowalsObjectField.toReference();
		this.shortTermBorrowalCountReference = borrowalsObjectField.field(
				"shortTermCount", f -> f.asInteger().sortable( Sortable.YES )
		)
				.toReference();
		this.longTermBorrowalCountReference = borrowalsObjectField.field(
				"longTermCount", f -> f.asInteger().sortable( Sortable.YES )
		)
				.toReference();
		this.totalBorrowalCountReference = borrowalsObjectField.field(
				"totalCount", f -> f.asInteger().sortable( Sortable.YES )
		)
				.toReference();
	}

	@Override
	public void write(DocumentElement target, Account account, TypeBridgeWriteContext context) {
		if ( account == null ) {
			return;
		}

		List<Borrowal> borrowals = account.getBorrowals();

		int shortTermBorrowalCount = 0;
		int longTermBorrowalCount = 0;
		for ( Borrowal borrowal : borrowals ) {
			switch ( borrowal.getType() ) {
				case SHORT_TERM:
					++shortTermBorrowalCount;
					break;
				case LONG_TERM:
					++longTermBorrowalCount;
					break;
			}
		}

		DocumentElement borrowalsObject = target.addObject( borrowalsObjectFieldReference );
		borrowalsObject.addValue( shortTermBorrowalCountReference, shortTermBorrowalCount );
		borrowalsObject.addValue( longTermBorrowalCountReference, longTermBorrowalCount );
		borrowalsObject.addValue( totalBorrowalCountReference, borrowals.size() );
	}

	public static class Binder implements TypeBinder {
		@Override
		public void bind(TypeBindingContext context) {
			context.dependencies()
					.use( "borrowals.type" );

			context.bridge( Account.class, new AccountBorrowalSummaryBridge( context.indexSchemaElement() ) );
		}
	}

}
