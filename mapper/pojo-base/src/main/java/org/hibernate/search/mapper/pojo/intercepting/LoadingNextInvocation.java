/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.intercepting;

/**
 * Next invocation.
 * @param <C> The contextural loading information
 */
public interface LoadingNextInvocation<C> {

	/**
	 * Proced to the next interceptor in the interceptor chain.For
	 * loading interceptor methods, the invocation of
	 * {@code proceed} in the last interceptor method in the chain causes
	 * the invocation of the target class method.
	 *
	 * @exception Exception if thrown by target method or interceptor method in call stack
	 */
	void proceed() throws Exception;

}
