/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.bridge;

import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBridgeBindingContext;
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

	private IndexObjectFieldReference borrowalsObjectFieldReference;
	private IndexFieldReference<Integer> shortTermBorrowalCountReference;
	private IndexFieldReference<Integer> longTermBorrowalCountReference;
	private IndexFieldReference<Integer> totalBorrowalCountReference;

	@Override
	public void bind(TypeBridgeBindingContext context) {
		context.getDependencies()
				.use( "borrowals.type" );

		IndexSchemaObjectField borrowalsObjectField = context.getIndexSchemaElement().objectField( "borrowals" );
		borrowalsObjectFieldReference = borrowalsObjectField.toReference();
		shortTermBorrowalCountReference = borrowalsObjectField.field(
				"shortTermCount", f -> f.asInteger().sortable( Sortable.YES )
		)
				.toReference();
		longTermBorrowalCountReference = borrowalsObjectField.field(
				"longTermCount", f -> f.asInteger().sortable( Sortable.YES )
		)
				.toReference();
		totalBorrowalCountReference = borrowalsObjectField.field(
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

}
