/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.bridge;

import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.mapper.model.SearchModel;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
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

	private PojoModelElementAccessor<Account> accountAccessor;
	private IndexObjectFieldAccessor borrowalsObjectFieldAccessor;
	private IndexFieldAccessor<Integer> shortTermBorrowalCountAccessor;
	private IndexFieldAccessor<Integer> longTermBorrowalCountAccessor;
	private IndexFieldAccessor<Integer> totalBorrowalCountAccessor;

	public AccountBorrowalSummaryBridge() {
	}

	@Override
	public void bind(IndexSchemaElement indexSchemaElement, PojoModelType bridgedPojoModelType,
			SearchModel searchModel) {
		// TODO allow to access collections properly, and more importantly to declare dependencies on parts of collection items
		accountAccessor = bridgedPojoModelType.createAccessor( Account.class );

		IndexSchemaObjectField borrowalsObjectField = indexSchemaElement.objectField( "borrowals" );
		borrowalsObjectFieldAccessor = borrowalsObjectField.createAccessor();
		shortTermBorrowalCountAccessor = borrowalsObjectField.field( "shortTermCount" ).asInteger()
				.sortable( Sortable.YES )
				.createAccessor();
		longTermBorrowalCountAccessor = borrowalsObjectField.field( "longTermCount" ).asInteger()
				.sortable( Sortable.YES )
				.createAccessor();
		totalBorrowalCountAccessor = borrowalsObjectField.field( "totalCount" ).asInteger()
				.sortable( Sortable.YES )
				.createAccessor();
	}

	@Override
	public void write(DocumentElement target, PojoElement source) {
		Account account = accountAccessor.read( source );
		if ( account == null ) {
			return;
		}

		// TODO this is bad, see the bind() method
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

		DocumentElement borrowalsObject = borrowalsObjectFieldAccessor.add( target );
		shortTermBorrowalCountAccessor.write( borrowalsObject, shortTermBorrowalCount );
		longTermBorrowalCountAccessor.write( borrowalsObject, longTermBorrowalCount );
		totalBorrowalCountAccessor.write( borrowalsObject, borrowals.size() );
	}

}
