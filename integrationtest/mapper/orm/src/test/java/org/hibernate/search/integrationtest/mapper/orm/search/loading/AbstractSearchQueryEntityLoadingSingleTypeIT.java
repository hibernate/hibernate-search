/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSoftAssertions;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.EntityIdDocumentIdIndexedEntity;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.NonEntityIdDocumentIdIndexedEntity;

public abstract class AbstractSearchQueryEntityLoadingSingleTypeIT<T> extends AbstractSearchQueryEntityLoadingIT {

	protected static List<SingleTypeLoadingModelPrimitives<?>> allSingleTypeLoadingModelPrimitives() {
		return Arrays.asList(
				new EntityIdDocumentIdLoadingModelPrimitives(),
				new NonEntityIdDocumentIdLoadingModelPrimitives()
		);
	}

	protected final SingleTypeLoadingModelPrimitives<T> primitives;

	AbstractSearchQueryEntityLoadingSingleTypeIT(SingleTypeLoadingModelPrimitives<T> primitives) {
		this.primitives = primitives;
	}

	protected final void testLoading(
			Consumer<Session> sessionSetup,
			Consumer<DocumentReferenceCollector> hitDocumentReferencesContributor,
			Consumer<EntityCollector<T>> expectedLoadedEntitiesContributor,
			Consumer<OrmSoftAssertions> assertionsContributor) {
		testLoading(
				sessionSetup,
				Collections.singletonList( primitives.getIndexedClass() ),
				Collections.singletonList( primitives.getIndexName() ),
				hitDocumentReferencesContributor,
				expectedLoadedEntitiesContributor,
				assertionsContributor
		);
	}

	protected abstract static class SingleTypeLoadingModelPrimitives<T> {

		public abstract String getIndexName();

		public abstract Class<T> getIndexedClass();

		public abstract T newIndexed(int id);

		public abstract String getDocumentIdForEntityId(int id);

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}
	}

	private static final class EntityIdDocumentIdLoadingModelPrimitives
			extends SingleTypeLoadingModelPrimitives<EntityIdDocumentIdIndexedEntity> {

		@Override
		public String getIndexName() {
			return EntityIdDocumentIdIndexedEntity.NAME;
		}

		@Override
		public Class<EntityIdDocumentIdIndexedEntity> getIndexedClass() {
			return EntityIdDocumentIdIndexedEntity.class;
		}

		@Override
		public EntityIdDocumentIdIndexedEntity newIndexed(int id) {
			return new EntityIdDocumentIdIndexedEntity( id );
		}

		@Override
		public String getDocumentIdForEntityId(int id) {
			return String.valueOf( id );
		}

	}

	private static final class NonEntityIdDocumentIdLoadingModelPrimitives
			extends SingleTypeLoadingModelPrimitives<NonEntityIdDocumentIdIndexedEntity> {

		@Override
		public String getIndexName() {
			return NonEntityIdDocumentIdIndexedEntity.NAME;
		}

		@Override
		public Class<NonEntityIdDocumentIdIndexedEntity> getIndexedClass() {
			return NonEntityIdDocumentIdIndexedEntity.class;
		}

		@Override
		public NonEntityIdDocumentIdIndexedEntity newIndexed(int id) {
			return new NonEntityIdDocumentIdIndexedEntity( id, getIntegerDocumentIdForEntityId( id ) );
		}

		@Override
		public String getDocumentIdForEntityId(int id) {
			return String.valueOf( getIntegerDocumentIdForEntityId( id ) );
		}

		private int getIntegerDocumentIdForEntityId(int id) {
			// Use a different document ID than the ID, to check that Search uses the right value when loading
			return id + 40;
		}

	}
}
