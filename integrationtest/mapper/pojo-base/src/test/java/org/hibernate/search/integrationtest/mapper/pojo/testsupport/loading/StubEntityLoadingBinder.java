/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
