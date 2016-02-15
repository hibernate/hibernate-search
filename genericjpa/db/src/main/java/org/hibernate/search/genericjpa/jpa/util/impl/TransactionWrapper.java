/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.jpa.util.impl;

/**
 * Created by Martin on 11.11.2015.
 */
public interface TransactionWrapper {

	void begin();

	void commit();

	void commitIgnoreExceptions();

	void rollback();

	void setIgnoreExceptionsForJTATransaction(boolean ignoreExceptionsForJTATransaction);

}
