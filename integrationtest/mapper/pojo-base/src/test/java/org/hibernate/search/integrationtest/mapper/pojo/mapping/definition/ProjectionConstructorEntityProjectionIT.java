/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.PersistenceTypeKey;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubLoadingContext;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading.StubSelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.EntityProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-4574")
public class ProjectionConstructorEntityProjectionIT extends AbstractProjectionConstructorIT {

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	protected final StubLoadingContext loadingContext = new StubLoadingContext();

	@Before
	public void persistEntities() {
		loadingContext.persistenceMap( IndexedEntity.PERSISTENCE_KEY )
				.put( 1, new IndexedEntity( 1, new Contained( 11 ) ) );
		loadingContext.persistenceMap( IndexedEntity.PERSISTENCE_KEY )
				.put( 2, new IndexedEntity( 2, new Contained( 21 ) ) );
		loadingContext.persistenceMap( IndexedEntity.PERSISTENCE_KEY )
				.put( 3, new IndexedEntity( 3, null ) );
	}

	@Test
	public void noArg() {
		class MyProjection {
			public final IndexedEntity entity;

			@ProjectionConstructor
			public MyProjection(@EntityProjection IndexedEntity entity) {
				this.entity = entity;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.withConfiguration( this::addLoadableEntityType )
				.setup();

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( StubBackendUtils.reference( IndexedEntity.NAME, "1" ) ),
						Arrays.asList( StubBackendUtils.reference( IndexedEntity.NAME, "2" ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.entity( IndexedEntity.class )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( loadingContext.persistenceMap( IndexedEntity.PERSISTENCE_KEY ).get( 1 ) ),
						new MyProjection( loadingContext.persistenceMap( IndexedEntity.PERSISTENCE_KEY ).get( 2 ) )
				)
		);
	}

	@Test
	public void supertype() {
		class MyProjection {
			public final Object entity;

			@ProjectionConstructor
			public MyProjection(@EntityProjection Object entity) {
				this.entity = entity;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.withConfiguration( this::addLoadableEntityType )
				.setup();

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( StubBackendUtils.reference( IndexedEntity.NAME, "1" ) ),
						Arrays.asList( StubBackendUtils.reference( IndexedEntity.NAME, "2" ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.entity( Object.class )
						)
						.asList(),
				Arrays.asList(
						new MyProjection( loadingContext.persistenceMap( IndexedEntity.PERSISTENCE_KEY ).get( 1 ) ),
						new MyProjection( loadingContext.persistenceMap( IndexedEntity.PERSISTENCE_KEY ).get( 2 ) )
				)
		);
	}

	@Test
	public void invalidType() {
		class MyProjection {
			public final Integer entity;

			@ProjectionConstructor
			public MyProjection(@EntityProjection Integer entity) {
				this.entity = entity;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.withConfiguration( this::addLoadableEntityType )
				.setup();

		try ( SearchSession session = createSession( mapping ) ) {
			assertThatThrownBy( () -> session.search( IndexedEntity.class )
					.select( MyProjection.class ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Invalid type for entity projection on type '" + IndexedEntity.NAME + "'",
							"the entity type's Java class '" + IndexedEntity.class.getName()
									+ "' does not extend the requested projection type '" + Integer.class.getName() + "'"
					);
		}
	}

	private void addLoadableEntityType(StandalonePojoMappingConfigurationContext context) {
		context.addEntityType( IndexedEntity.class, IndexedEntity.NAME, c -> c.selectionLoadingStrategy(
				new StubSelectionLoadingStrategy<>( IndexedEntity.PERSISTENCE_KEY ) ) );
	}

	@Override
	protected SearchSession createSession(SearchMapping mapping) {
		return mapping.createSessionWithOptions()
				.loading( o -> o.context( StubLoadingContext.class, loadingContext ) )
				.build();
	}

	@Indexed(index = INDEX_NAME)
	static class IndexedEntity {
		public static final String NAME = "IndexedEntity";
		public static final PersistenceTypeKey<IndexedEntity, Integer> PERSISTENCE_KEY =
				new PersistenceTypeKey<>( IndexedEntity.class, Integer.class );

		@DocumentId
		public Integer id;
		@FullTextField
		public String text;
		@IndexedEmbedded
		public Contained contained;

		public IndexedEntity(int id, Contained contained) {
			this.id = id;
			this.text = "text#" + id;
			this.contained = contained;
		}
	}

	static class Contained {
		@DocumentId
		public Integer id;
		@FullTextField
		public String text;

		public Contained(Integer id) {
			this.id = id;
			this.text = "text#" + id;
		}
	}

}
