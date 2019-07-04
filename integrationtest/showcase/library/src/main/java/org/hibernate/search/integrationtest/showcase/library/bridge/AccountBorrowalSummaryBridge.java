/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.bridge;

import java.lang.annotation.Annotation;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.integrationtest.showcase.library.model.Account;
import org.hibernate.search.integrationtest.showcase.library.model.Borrowal;

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
public class AccountBorrowalSummaryBridge implements TypeBridge {

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
	public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) {
		Account account = (Account) bridgedElement;
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

	public static class Binder implements TypeBinder<Annotation> {
		@Override
		public void bind(TypeBindingContext context) {
			context.getDependencies()
					.use( "borrowals.type" );

			context.setBridge(
					new AccountBorrowalSummaryBridge( context.getIndexSchemaElement() )
			);
		}
	}

}
