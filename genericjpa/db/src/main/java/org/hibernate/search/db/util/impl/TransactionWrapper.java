/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.db.util.impl;

/**
 * Instances of this class are used for easier Transaction handling.
 * In a general JPA context we would have to decide whether to use a
 * JTA transaction or the the JPA transaction. This is encapsulated away in
 * instances of this interface.
 *
 * @author Martin Braun
 */
public interface TransactionWrapper {

	void begin();

	void commit();

	void commitIgnoreExceptions();

	void rollback();

	void setIgnoreExceptionsForJTATransaction(boolean ignoreExceptionsForJTATransaction);

}
