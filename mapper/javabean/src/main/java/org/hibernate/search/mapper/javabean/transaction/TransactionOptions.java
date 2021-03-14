/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.transaction;

/**
 * Options a transaction for a loading or indexing proccess.
 */
public interface TransactionOptions {

	/**
	 * @return the transaction timeout
	 */
	Integer transactionTimeout();
}
