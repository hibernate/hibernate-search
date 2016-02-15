/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.transaction;

import javax.transaction.TransactionManager;
import java.util.Map;

import org.hibernate.search.genericjpa.Constants;

/**
 * Utility interface to abstract the lookup of a TransactionManager. Users can specify their own logic for lookup, by implementing it and setting {@value #TRANSACTION_MANAGER_CLASS_KEY}
 */
public interface TransactionManagerProvider {

	String TRANSACTION_MANAGER_CLASS_KEY = Constants.TRANSACTION_MANAGER_PROVIDER_KEY;

	TransactionManager get(ClassLoader classLoader, Map properties);

}
