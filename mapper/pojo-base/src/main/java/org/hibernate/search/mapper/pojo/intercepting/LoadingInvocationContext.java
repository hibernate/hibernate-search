/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.intercepting;

import java.util.Map;

/**
 * Exposes contextual information about the intercepted invocation and
 * operations that enable interceptor methods to control the behavior
 * of the invocation chain.
 * @param <O> The options for loading process
 */
public interface LoadingInvocationContext<O> {

	/**
	 * @return The mass options for loading process.
	 */
	O options();

	/**
	 * @return the identifier of the tenant.
	 */
	String tenantId();

	/**
	 * Evaluates this predicate on the intercepting process.
	 *
	 * @param active a predicate that will be test intercepting active process
	 */
	void active(LoadingProcessActivePredicate active);

	/**
	 * Return this predicate on the intercepting process.
	 *
	 * @return a predicate that will be test intercepting active process
	 */
	LoadingProcessActivePredicate active();

	/**
	 * Enables an interceptor to retrieve or update the data associated with the invocation by another interceptor.
	 *
	 * @return the context data associated with this invocation or
	 * lifecycle callback. If there is no context data, an
	 * empty {@code Map<String,Object>} object will be returned.
	 */
	Map<Object, Object> contextData();

	/**
	 * Invoke to the next interceptor in the interceptor chain.For mass options interceptor methods, the invocation of
	 * {@code proceed} in the last interceptor method in the chain causes
	 * the invocation of the target class method.
	 *
	 * @exception Exception if thrown by target method or interceptor method in call stack
	 */
	void proceed() throws Exception;

	/**
	 * Invoke to the next interceptor in the interceptor chain.For mass options interceptor methods, the invocation of
	 * {@code proceed} in the last interceptor method in the chain causes
	 * the invocation of the target class method.
	 *
	 * @param next next finalize intercepting method
	 * @exception Exception if thrown by target method or interceptor method in call stack
	 */
	void proceed(LoadingInvocationInterceptor next) throws Exception;
}
