/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.definition.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.search.projection.definition.spi.CompositeProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.spi.ProjectionRegistry;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class PojoSearchQueryElementRegistry implements ProjectionRegistry, AutoCloseable {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<Class<?>, CompositeProjectionDefinition<?>> compositeProjectionDefinitions;

	public PojoSearchQueryElementRegistry(Map<Class<?>, CompositeProjectionDefinition<?>> compositeProjectionDefinitions) {
		this.compositeProjectionDefinitions = compositeProjectionDefinitions;
	}

	@Override
	public <T> CompositeProjectionDefinition<T> composite(Class<T> objectClass) {
		Optional<CompositeProjectionDefinition<T>> definition = compositeOptional( objectClass );
		if ( !definition.isPresent() ) {
			throw log.invalidObjectClassForProjection( objectClass );
		}
		return definition.get();
	}

	@Override
	public <T> Optional<CompositeProjectionDefinition<T>> compositeOptional(Class<T> objectClass) {
		@SuppressWarnings("unchecked") // By construction, we know the definition has that type if it exists.
		CompositeProjectionDefinition<T> definition =
				(CompositeProjectionDefinition<T>) compositeProjectionDefinitions.get( objectClass );
		return Optional.ofNullable( definition );
	}

	@Override
	public void close() throws Exception {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( CompositeProjectionDefinition::close, compositeProjectionDefinitions.values() );
		}
	}
}
