/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading;

import java.util.Locale;

import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBindingContext;

public class StubEntityLoadingBinder implements EntityLoadingBinder {
	public StubEntityLoadingBinder() {
	}

	@Override
	public void bind(EntityLoadingBindingContext context) {
		Class<?> entityClass = context.entityType().rawType();
		PersistenceTypeKey<?, ?> key;
		try {
			key = (PersistenceTypeKey<?, ?>) entityClass.getField( "PERSISTENCE_KEY" ).get( null );
		}
		catch (IllegalAccessException | NoSuchFieldException | RuntimeException e) {
			throw new RuntimeException( String.format( Locale.ROOT,
					"Could not find static field PERSISTENCE_KEY in %s; possible wrong test setup? Cause: %s",
					entityClass, e.getMessage()
			), e );
		}
		bind( context, key );
	}

	private <E, I> void bind(EntityLoadingBindingContext context, PersistenceTypeKey<E, I> key) {
		context.selectionLoadingStrategy( key.entityType, new StubSelectionLoadingStrategy<>( key ) );
		context.massLoadingStrategy( key.entityType, new StubMassLoadingStrategy<>( key ) );
	}
}
