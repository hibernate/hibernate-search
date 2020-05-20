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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.EntityIdDocumentIdContainedEntity;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.EntityIdDocumentIdIndexedEntity;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.NonEntityIdDocumentIdContainedEntity;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.NonEntityIdDocumentIdIndexedEntity;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSoftAssertions;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

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

	protected final void persistThatManyEntities(int entityCount) {
		// We don't care about what is indexed exactly, so use the lenient mode
		backendMock.inLenientMode( () -> OrmUtils.withinTransaction( sessionFactory(), session -> {
			for ( int i = 0; i < entityCount; i++ ) {
				session.persist( primitives.newIndexed( i ) );
			}
		} ) );
	}

	protected final void testLoadingThatManyEntities(
			Consumer<Session> sessionSetup,
			Consumer<SearchLoadingOptionsStep> loadingOptionsContributor,
			int entityCount,
			Consumer<OrmSoftAssertions> assertionsContributor) {
		testLoading(
				sessionSetup,
				Collections.singletonList( primitives.getIndexedClass() ),
				Collections.singletonList( primitives.getIndexName() ),
				loadingOptionsContributor,
				c -> {
					for ( int i = 0; i < entityCount; i++ ) {
						c.doc( primitives.getIndexName(), primitives.getDocumentIdForEntityId( i ) );
					}
				},
				c -> {
					for ( int i = 0; i < entityCount; i++ ) {
						c.entity( primitives.getIndexedClass(), i );
					}
				},
				assertionsContributor
		);
	}

	protected final void testLoading(
			Consumer<Session> sessionSetup,
			Consumer<SearchLoadingOptionsStep> loadingOptionsContributor,
			Consumer<DocumentReferenceCollector> hitDocumentReferencesContributor,
			Consumer<EntityCollector<T>> expectedLoadedEntitiesContributor,
			Consumer<OrmSoftAssertions> assertionsContributor) {
		testLoading(
				sessionSetup,
				Collections.singletonList( primitives.getIndexedClass() ),
				Collections.singletonList( primitives.getIndexName() ),
				loadingOptionsContributor,
				hitDocumentReferencesContributor,
				expectedLoadedEntitiesContributor,
				assertionsContributor
		);
	}

	protected final void testLoading(
			Consumer<Session> sessionSetup,
			Consumer<SearchLoadingOptionsStep> loadingOptionsContributor,
			Consumer<DocumentReferenceCollector> hitDocumentReferencesContributor,
			Consumer<EntityCollector<T>> expectedLoadedEntitiesContributor,
			BiConsumer<OrmSoftAssertions, List<T>> assertionsContributor) {
		testLoading(
				sessionSetup,
				Collections.singletonList( primitives.getIndexedClass() ),
				Collections.singletonList( primitives.getIndexName() ),
				loadingOptionsContributor,
				hitDocumentReferencesContributor,
				expectedLoadedEntitiesContributor,
				assertionsContributor
		);
	}

	protected abstract static class SingleTypeLoadingModelPrimitives<T> {

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}

		public abstract String getIndexName();

		public abstract Class<T> getIndexedClass();

		public abstract Class<?>[] getEntityClasses();

		public abstract String getIndexedEntityName();

		public abstract boolean isCacheLookupSupported();

		public abstract String getEagerGraphName();

		public abstract String getLazyGraphName();

		public abstract T newIndexed(int id);

		public abstract T newIndexedWithContained(int id);

		public abstract String getDocumentIdForEntityId(int id);

		public abstract Object getContainedEager(T entity);

		public abstract List<?> getContainedLazy(T entity);

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
		public Class<?>[] getEntityClasses() {
			return new Class[] { EntityIdDocumentIdIndexedEntity.class, EntityIdDocumentIdContainedEntity.class };
		}

		@Override
		public String getIndexedEntityName() {
			return EntityIdDocumentIdIndexedEntity.NAME;
		}

		@Override
		public boolean isCacheLookupSupported() {
			return true;
		}

		@Override
		public String getEagerGraphName() {
			return EntityIdDocumentIdIndexedEntity.GRAPH_EAGER;
		}

		@Override
		public String getLazyGraphName() {
			return EntityIdDocumentIdIndexedEntity.GRAPH_LAZY;
		}

		@Override
		public EntityIdDocumentIdIndexedEntity newIndexed(int id) {
			return new EntityIdDocumentIdIndexedEntity( id );
		}

		@Override
		public EntityIdDocumentIdIndexedEntity newIndexedWithContained(int id) {
			EntityIdDocumentIdIndexedEntity entity = newIndexed( id );
			EntityIdDocumentIdContainedEntity containedEager = new EntityIdDocumentIdContainedEntity( id * 10000 );
			entity.setContainedEager( containedEager );
			containedEager.setContainingEager( entity );
			EntityIdDocumentIdContainedEntity containedLazy = new EntityIdDocumentIdContainedEntity( id * 10000 + 1 );
			entity.getContainedLazy().add( containedLazy );
			containedLazy.setContainingLazy( entity );
			return entity;
		}

		@Override
		public String getDocumentIdForEntityId(int id) {
			return String.valueOf( id );
		}

		@Override
		public Object getContainedEager(EntityIdDocumentIdIndexedEntity entity) {
			return entity.getContainedEager();
		}

		@Override
		public List<?> getContainedLazy(EntityIdDocumentIdIndexedEntity entity) {
			return entity.getContainedLazy();
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
		public Class<?>[] getEntityClasses() {
			return new Class[] { NonEntityIdDocumentIdIndexedEntity.class, NonEntityIdDocumentIdContainedEntity.class };
		}

		@Override
		public String getIndexedEntityName() {
			return NonEntityIdDocumentIdIndexedEntity.NAME;
		}

		@Override
		public boolean isCacheLookupSupported() {
			return false;
		}

		@Override
		public String getEagerGraphName() {
			return NonEntityIdDocumentIdIndexedEntity.GRAPH_EAGER;
		}

		@Override
		public String getLazyGraphName() {
			return NonEntityIdDocumentIdIndexedEntity.GRAPH_LAZY;
		}

		@Override
		public NonEntityIdDocumentIdIndexedEntity newIndexed(int id) {
			return new NonEntityIdDocumentIdIndexedEntity( id, getIntegerDocumentIdForEntityId( id ) );
		}

		@Override
		public NonEntityIdDocumentIdIndexedEntity newIndexedWithContained(int id) {
			NonEntityIdDocumentIdIndexedEntity entity = newIndexed( id );
			NonEntityIdDocumentIdContainedEntity containedEager = new NonEntityIdDocumentIdContainedEntity( id * 10000 );
			entity.setContainedEager( containedEager );
			containedEager.setContainingEager( entity );
			NonEntityIdDocumentIdContainedEntity containedLazy = new NonEntityIdDocumentIdContainedEntity( id * 10000 + 1 );
			entity.getContainedLazy().add( containedLazy );
			containedLazy.setContainingLazy( entity );
			return entity;
		}

		@Override
		public String getDocumentIdForEntityId(int id) {
			return String.valueOf( getIntegerDocumentIdForEntityId( id ) );
		}

		private int getIntegerDocumentIdForEntityId(int id) {
			// Use a different document ID than the ID, to check that Search uses the right value when loading
			return id + 40;
		}

		@Override
		public Object getContainedEager(NonEntityIdDocumentIdIndexedEntity entity) {
			return entity.getContainedEager();
		}

		@Override
		public List<?> getContainedLazy(NonEntityIdDocumentIdIndexedEntity entity) {
			return entity.getContainedLazy();
		}
	}
}
