/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.transaction.impl;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import java.util.Map;

import org.hibernate.search.genericjpa.exception.SearchException;
import org.hibernate.search.genericjpa.transaction.TransactionManagerProvider;

import static org.hibernate.search.genericjpa.Constants.TRANSACTION_MANAGER_JNDI_KEY;

/**
 * {@link TransactionManagerProvider} that looks up the TransactionManager per JNDI lookup in {@link InitialContext} of the property specified value in {@value #JNDI_KEY}
 */
public class JNDILookupTransactionManagerProvider implements TransactionManagerProvider {

	private static final String JNDI_KEY = TRANSACTION_MANAGER_JNDI_KEY;

	@Override
	public TransactionManager get(ClassLoader classLoader, Map properties) {
		String jndiName = (String) properties.get( JNDI_KEY );
		if ( jndiName == null ) {
			throw new SearchException( JNDI_KEY + " must not be null if using: " + JNDILookupTransactionManagerProvider.class );
		}
		TransactionManager ret;
		try {
			ret = InitialContext.doLookup( jndiName );
		}
		catch (NamingException e) {
			throw new SearchException( "error while looking up " + jndiName );
		}
		if ( ret == null ) {
			throw new SearchException( jndiName + "was not found!" );
		}
		return ret;
	}

}
