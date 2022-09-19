/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.hibernateormapis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.ManagedAssert.assertThatManaged;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.QueryTimeoutException;

import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.Query;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * Test the compatibility layer between our APIs and Hibernate ORM APIs
 * for the {@link Query} class.
 */
public class ToHibernateOrmQueryIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext, ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
		backendMock.expectAnySchema( IndexedEntity.NAME );
		setupContext.withAnnotatedTypes( IndexedEntity.class, ContainedEntity.class );

		dataClearConfig.preClear( ContainedEntity.class, c -> {
			c.setContainingLazy( null );
		} );
		dataClearConfig.clearOrder( IndexedEntity.class, ContainedEntity.class );
	}

	@Before
	public void initData() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity indexed1 = new IndexedEntity();
			indexed1.setId( 1 );
			indexed1.setText( "this is text (1)" );
			ContainedEntity contained1_1 = new ContainedEntity();
			contained1_1.setId( 11 );
			contained1_1.setText( "this is text (1_1)" );
			indexed1.setContainedEager( contained1_1 );
			contained1_1.setContainingEager( indexed1 );
			ContainedEntity contained1_2 = new ContainedEntity();
			contained1_2.setId( 12 );
			contained1_2.setText( "this is text (1_2)" );
			indexed1.getContainedLazy().add( contained1_2 );
			contained1_2.setContainingLazy( indexed1 );

			IndexedEntity indexed2 = new IndexedEntity();
			indexed2.setId( 2 );
			indexed2.setText( "some more text (2)" );
			ContainedEntity contained2_1 = new ContainedEntity();
			contained2_1.setId( 21 );
			contained2_1.setText( "this is text (2_1)" );
			indexed2.setContainedEager( contained2_1 );
			contained2_1.setContainingEager( indexed2 );
			ContainedEntity contained2_2 = new ContainedEntity();
			contained2_2.setId( 22 );
			contained2_2.setText( "this is text (2_2)" );
			indexed2.getContainedLazy().add( contained2_2 );
			contained2_2.setContainingLazy( indexed2 );

			session.persist( contained1_1 );
			session.persist( contained1_2 );
			session.persist( indexed1 );
			session.persist( contained2_1 );
			session.persist( contained2_2 );
			session.persist( indexed2 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "text", indexed1.getText() )
							.objectField( "containedEager", b2 -> b2
									.field( "text", contained1_1.getText() )
							)
							.objectField( "containedLazy", b2 -> b2
									.field( "text", contained1_2.getText() )
							)
					)
					.add( "2", b -> b
							.field( "text", indexed2.getText() )
							.objectField( "containedEager", b2 -> b2
									.field( "text", contained2_1.getText() )
							)
							.objectField( "containedLazy", b2 -> b2
									.field( "text", contained2_2.getText() )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void toHibernateOrmQuery() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );
			assertThat( query ).isNotNull();
		} );
	}

	@Test
	public void list() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							6L,
							reference( IndexedEntity.NAME, "1" ),
							reference( IndexedEntity.NAME, "2" )
					)
			);
			List<IndexedEntity> result = query.list();
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.containsExactly(
							session.getReference( IndexedEntity.class, 1 ),
							session.getReference( IndexedEntity.class, 2 )
					);
		} );
	}

	@Test
	public void uniqueResult() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							1L,
							reference( IndexedEntity.NAME, "1" )
					)
			);
			IndexedEntity result = query.uniqueResult();
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.isEqualTo( session.getReference( IndexedEntity.class, 1 ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> { },
					StubSearchWorkBehavior.empty()
			);
			result = query.uniqueResult();
			backendMock.verifyExpectationsMet();
			assertThat( result ).isNull();

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							2L,
							reference( IndexedEntity.NAME, "1" ),
							reference( IndexedEntity.NAME, "2" )
					)
			);
			assertThatThrownBy( () -> query.uniqueResult() )
					.isInstanceOf( org.hibernate.NonUniqueResultException.class );
			backendMock.verifyExpectationsMet();

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							2L,
							reference( IndexedEntity.NAME, "1" ),
							reference( IndexedEntity.NAME, "1" )
					)
			);
			result = query.uniqueResult();
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.isEqualTo( session.getReference( IndexedEntity.class, 1 ) );
		} );
	}

	@Test
	public void pagination() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			assertThat( query.getFirstResult() ).isEqualTo( 0 );
			assertThat( query.getMaxResults() ).isEqualTo( Integer.MAX_VALUE );

			query.setFirstResult( 3 );
			query.setMaxResults( 2 );

			assertThat( query.getFirstResult() ).isEqualTo( 3 );
			assertThat( query.getMaxResults() ).isEqualTo( 2 );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> b
							.offset( 3 )
							.limit( 2 ),
					StubSearchWorkBehavior.empty()
			);
			query.list();
			backendMock.verifyExpectationsMet();
		} );
	}

	@Test
	public void timeout_dsl() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.matchAll() )
							.failAfter( 2, TimeUnit.SECONDS )
							.toQuery()
			);

			SearchTimeoutException timeoutException = new SearchTimeoutException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> b.failAfter( 2, TimeUnit.SECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			assertThatThrownBy( () -> query.list() )
					.isInstanceOf( QueryTimeoutException.class )
					.hasCause( timeoutException );
		} );
	}

	@Test
	public void timeout_jpaHint() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.setHint( "javax.persistence.query.timeout", 200 );

			SearchTimeoutException timeoutException = new SearchTimeoutException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> b.failAfter( 200, TimeUnit.MILLISECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			assertThatThrownBy( () -> query.list() )
					.isInstanceOf( QueryTimeoutException.class )
					.hasCause( timeoutException );
		} );
	}

	@Test
	public void timeout_ormHint() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.setHint( "org.hibernate.timeout", 4 );

			SearchTimeoutException timeoutException = new SearchTimeoutException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> b.failAfter( 4, TimeUnit.SECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			assertThatThrownBy( () -> query.list() )
					.isInstanceOf( QueryTimeoutException.class )
					.hasCause( timeoutException );
		} );
	}

	@Test
	public void timeout_setter() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.setTimeout( 3 );

			SearchTimeoutException timeoutException = new SearchTimeoutException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> b.failAfter( 3, TimeUnit.SECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			assertThatThrownBy( () -> query.list() )
					.isInstanceOf( QueryTimeoutException.class )
					.hasCause( timeoutException );
		} );
	}

	@Test
	public void timeout_override_ormHint() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.matchAll() )
							.failAfter( 2, TimeUnit.SECONDS )
							.toQuery()
			);

			query.setHint( "org.hibernate.timeout", 4 );

			SearchTimeoutException timeoutException = new SearchTimeoutException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> b.failAfter( 4, TimeUnit.SECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			assertThatThrownBy( () -> query.list() )
					.isInstanceOf( QueryTimeoutException.class )
					.hasCause( timeoutException );
		} );
	}

	@Test
	public void timeout_override_setter() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.matchAll() )
							.failAfter( 2, TimeUnit.SECONDS )
							.toQuery()
			);

			query.setTimeout( 3 );

			SearchTimeoutException timeoutException = new SearchTimeoutException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> b.failAfter( 3, TimeUnit.SECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			assertThatThrownBy( () -> query.list() )
					.isInstanceOf( QueryTimeoutException.class )
					.hasCause( timeoutException );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graph_jpaHint_fetch() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.setHint( "javax.persistence.fetchgraph", session.getEntityGraph( IndexedEntity.GRAPH_EAGER ) );

			backendMock.expectSearchObjects(
					IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.uniqueResult();
			assertThatManaged( loaded.getContainedEager() ).isInitialized();
			assertThatManaged( loaded.getContainedLazy() ).isInitialized();
		} );

		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.setHint( "javax.persistence.fetchgraph", session.getEntityGraph( IndexedEntity.GRAPH_LAZY ) );

			backendMock.expectSearchObjects(
					IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.uniqueResult();
			// FETCH graph => associations can be forced to lazy even if eager in the mapping
			assertThatManaged( loaded.getContainedEager() ).isNotInitialized();
			assertThatManaged( loaded.getContainedLazy() ).isNotInitialized();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graph_jpaHint_load() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.setHint( "javax.persistence.loadgraph", session.getEntityGraph( IndexedEntity.GRAPH_EAGER ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ), b -> { },
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.uniqueResult();
			assertThatManaged( loaded.getContainedEager() ).isInitialized();
			assertThatManaged( loaded.getContainedLazy() ).isInitialized();
		} );

		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.setHint( "javax.persistence.loadgraph", session.getEntityGraph( IndexedEntity.GRAPH_LAZY ) );

			backendMock.expectSearchObjects(
					IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.uniqueResult();
			// LOAD graph => associations cannot be forced to lazy if eager in the mapping
			assertThatManaged( loaded.getContainedEager() ).isInitialized();
			assertThatManaged( loaded.getContainedLazy() ).isNotInitialized();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graph_setter_fetch() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.applyFetchGraph( session.getEntityGraph( IndexedEntity.GRAPH_EAGER ) );

			backendMock.expectSearchObjects(
					IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.uniqueResult();
			assertThatManaged( loaded.getContainedEager() ).isInitialized();
			assertThatManaged( loaded.getContainedLazy() ).isInitialized();
		} );

		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.applyFetchGraph( session.getEntityGraph( IndexedEntity.GRAPH_LAZY ) );

			backendMock.expectSearchObjects(
					IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.uniqueResult();
			// FETCH graph => associations can be forced to lazy even if eager in the mapping
			assertThatManaged( loaded.getContainedEager() ).isNotInitialized();
			assertThatManaged( loaded.getContainedLazy() ).isNotInitialized();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graph_setter_load() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.applyLoadGraph( session.getEntityGraph( IndexedEntity.GRAPH_EAGER ) );

			backendMock.expectSearchObjects(
					IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.uniqueResult();
			assertThatManaged( loaded.getContainedEager() ).isInitialized();
			assertThatManaged( loaded.getContainedLazy() ).isInitialized();
		} );

		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.applyLoadGraph( session.getEntityGraph( IndexedEntity.GRAPH_LAZY ) );

			backendMock.expectSearchObjects(
					IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.uniqueResult();
			// LOAD graph => associations cannot be forced to lazy if eager in the mapping
			assertThatManaged( loaded.getContainedEager() ).isInitialized();
			assertThatManaged( loaded.getContainedLazy() ).isNotInitialized();
		} );
	}

	@Test
	public void graph_override_jpaHint() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.matchAll() )
							.loading( o -> o.graph( IndexedEntity.GRAPH_EAGER, GraphSemantic.LOAD ) )
							.toQuery()
			);

			query.setHint( "javax.persistence.fetchgraph", session.getEntityGraph( IndexedEntity.GRAPH_LAZY ) );

			backendMock.expectSearchObjects(
					IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.uniqueResult();
			// FETCH graph => associations can be forced to lazy even if eager in the mapping
			assertThatManaged( loaded.getContainedEager() ).isNotInitialized();
			assertThatManaged( loaded.getContainedLazy() ).isNotInitialized();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graph_override_setter() {
		setupHolder.runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			Query<IndexedEntity> query = Search.toOrmQuery(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.matchAll() )
							.loading( o -> o.graph( IndexedEntity.GRAPH_EAGER, GraphSemantic.LOAD ) )
							.toQuery()
			);

			query.applyFetchGraph( session.getEntityGraph( IndexedEntity.GRAPH_LAZY ) );

			backendMock.expectSearchObjects(
					IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.uniqueResult();
			// FETCH graph => associations can be forced to lazy even if eager in the mapping
			assertThatManaged( loaded.getContainedEager() ).isNotInitialized();
			assertThatManaged( loaded.getContainedLazy() ).isNotInitialized();
		} );
	}

	private SearchQuery<IndexedEntity> createSimpleQuery(SearchSession searchSession) {
		return searchSession.search( IndexedEntity.class )
				.selectEntity()
				.where( f -> f.matchAll() )
				.toQuery();
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	@NamedEntityGraph(
			name = IndexedEntity.GRAPH_EAGER,
			includeAllAttributes = true
	)
	@NamedEntityGraph(
			name = IndexedEntity.GRAPH_LAZY
	)
	public static class IndexedEntity {

		public static final String NAME = "indexed";

		public static final String GRAPH_EAGER = "graph-eager";
		public static final String GRAPH_LAZY = "graph-lazy";

		@Id
		private Integer id;

		@FullTextField
		private String text;

		@OneToOne(fetch = FetchType.EAGER)
		@IndexedEmbedded
		private ContainedEntity containedEager;

		@OneToMany(mappedBy = "containingLazy", fetch = FetchType.LAZY)
		@IndexedEmbedded
		private List<ContainedEntity> containedLazy = new ArrayList<>();

		@Override
		public String toString() {
			return "IndexedEntity[id=" + id + "]";
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public ContainedEntity getContainedEager() {
			return containedEager;
		}

		public void setContainedEager(ContainedEntity containedEager) {
			this.containedEager = containedEager;
		}

		public List<ContainedEntity> getContainedLazy() {
			return containedLazy;
		}
	}

	@Entity(name = ContainedEntity.NAME)
	public static class ContainedEntity {

		public static final String NAME = "contained";

		@Id
		private Integer id;

		@FullTextField
		private String text;

		@OneToOne(mappedBy = "containedEager")
		private IndexedEntity containingEager;

		@ManyToOne
		private IndexedEntity containingLazy;

		@Override
		public String toString() {
			return "ContainedEntity[id=" + id + "]";
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public IndexedEntity getContainingEager() {
			return containingEager;
		}

		public void setContainingEager(IndexedEntity containingEager) {
			this.containingEager = containingEager;
		}

		public IndexedEntity getContainingLazy() {
			return containingLazy;
		}

		public void setContainingLazy(IndexedEntity containingLazy) {
			this.containingLazy = containingLazy;
		}
	}

}
