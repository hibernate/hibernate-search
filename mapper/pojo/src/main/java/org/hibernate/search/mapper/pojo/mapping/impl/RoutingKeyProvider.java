/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.function.Supplier;

/**
 * @author Yoann Rodiere
 */
public interface RoutingKeyProvider<E> extends AutoCloseable {

	@Override
	default void close() {
	}

	String toRoutingKey(String tenantIdentifier, Object identifier, Supplier<E> entitySupplier);

	static <E> RoutingKeyProvider<E> alwaysNull() {
		return (identifier, tenantIdentifier, entity) -> null;
	}

}
