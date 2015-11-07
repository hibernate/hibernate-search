/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa;

import org.hibernate.search.genericjpa.db.events.jpa.impl.AsyncUpdateConstants;
import org.hibernate.search.genericjpa.impl.SearchFactoryRegistry;
import org.hibernate.search.genericjpa.transaction.impl.JNDILookupTransactionManagerProvider;

/**
 * @author Martin Braun
 */
public class Constants {

	public static final String USE_JTA_TRANSACTIONS_KEY = "hibernate.search.useJTATransactions";
	public static final String USE_JTA_TRANSACTIONS_DEFAULT_VALUE = "false";
	public static final String SEARCH_FACTORY_TYPE_KEY = "hibernate.search.searchfactory.type";
	public static final String SEARCH_FACTORY_TYPE_DEFAULT_VALUE = "sql";
	public static final String BATCH_SIZE_FOR_UPDATE_QUERIES_KEY = AsyncUpdateConstants.BATCH_SIZE_FOR_UPDATE_QUERIES_KEY;
	public static final String BATCH_SIZE_FOR_UPDATE_QUERIES_DEFAULT_VALUE = AsyncUpdateConstants.BATCH_SIZE_FOR_UPDATE_QUERIES_DEFAULT_VALUE;
	public static final String ADDITIONAL_INDEXED_TYPES_KEY = "hibernate.search.additionalIndexedTypes";
	public static final String SEARCH_FACTORY_NAME_KEY = SearchFactoryRegistry.NAME_PROPERTY;
	public static final String SEARCH_FACTORY_NAME_DEFAULT_VALUE = SearchFactoryRegistry.DEFAULT_NAME;
	public static final String TRANSACTION_MANAGER_PROVIDER_KEY = "hibernate.search.transactionManagerProvider";
	public static final String TRANSACTION_MANAGER_PROVIDER_DEFAULT_VALUE = JNDILookupTransactionManagerProvider.class.getName();
	public static final String TRANSACTION_MANAGER_JNDI_KEY = "hibernate.search.transactionManagerProvider.jndi";

	public static final String UPDATE_DELAY_KEY = AsyncUpdateConstants.UPDATE_DELAY_KEY;
	public static final String UPDATE_DELAY_DEFAULT_VALUE = AsyncUpdateConstants.UPDATE_DELAY_DEFAULT_VALUE;
	public static final String BATCH_SIZE_FOR_UPDATES_KEY = AsyncUpdateConstants.BATCH_SIZE_FOR_UPDATES_KEY;
	public static final String BATCH_SIZE_FOR_UPDATES_DEFAULT_VALUE = AsyncUpdateConstants.BATCH_SIZE_FOR_UPDATES_DEFAULT_VALUE;

	public static final String TRIGGER_SOURCE_KEY = AsyncUpdateConstants.TRIGGER_SOURCE_KEY;

	public static final String TRIGGER_CREATION_STRATEGY_KEY = AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_KEY;
	public static final String TRIGGER_CREATION_STRATEGY_CREATE = AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_CREATE;
	public static final String TRIGGER_CREATION_STRATEGY_DROP_CREATE = AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_DROP_CREATE;
	public static final String TRIGGER_CREATION_STRATEGY_DONT_CREATE = AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_DONT_CREATE;
	public static final String TRIGGER_CREATION_STRATEGY_DEFAULT_VALUE = AsyncUpdateConstants.TRIGGER_CREATION_STRATEGY_DEFAULT_VALUE;

	private Constants() {
		// can't touch this!
	}

}
