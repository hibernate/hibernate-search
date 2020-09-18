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
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinEntityManager;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinJPATransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.QueryTimeoutException;
import javax.persistence.TypedQuery;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.graph.GraphSemantic;
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
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the compatibility layer between our APIs and JPA APIs
 * for the {@link TypedQuery} class.
 */
public class ToJpaQueryIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( IndexedEntity.NAME );
		entityManagerFactory = ormSetupHelper.start()
				.withProperty( AvailableSettings.JPA_QUERY_COMPLIANCE, true )
				.setup( IndexedEntity.class, ContainedEntity.class );
		backendMock.verifyExpectationsMet();

		withinJPATransaction( entityManagerFactory, entityManager -> {
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

			entityManager.persist( contained1_1 );
			entityManager.persist( contained1_2 );
			entityManager.persist( indexed1 );
			entityManager.persist( contained2_1 );
			entityManager.persist( contained2_2 );
			entityManager.persist( indexed2 );

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
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void toJpaQuery() {
		withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toJpaQuery( createSimpleQuery( searchSession ) );
			assertThat( query ).isNotNull();
		} );
	}

	@Test
	public void getResultList() {
		withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toJpaQuery( createSimpleQuery( searchSession ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							6L,
							reference( IndexedEntity.NAME, "1" ),
							reference( IndexedEntity.NAME, "2" )
					)
			);
			List<IndexedEntity> result = query.getResultList();
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.containsExactly(
							entityManager.getReference( IndexedEntity.class, 1 ),
							entityManager.getReference( IndexedEntity.class, 2 )
					);
		} );
	}

	@Test
	public void getSingleResult() {
		withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toJpaQuery( createSimpleQuery( searchSession ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							1L,
							reference( IndexedEntity.NAME, "1" )
					)
			);
			IndexedEntity result = query.getSingleResult();
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.isEqualTo( entityManager.getReference( IndexedEntity.class, 1 ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> { },
					StubSearchWorkBehavior.empty()
			);
			assertThatThrownBy( () -> query.getSingleResult() )
					.isInstanceOf( NoResultException.class );
			backendMock.verifyExpectationsMet();

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							2L,
							reference( IndexedEntity.NAME, "1" ),
							reference( IndexedEntity.NAME, "2" )
					)
			);
			assertThatThrownBy( () -> query.getSingleResult() )
					.isInstanceOf( NonUniqueResultException.class );
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
			result = query.getSingleResult();
			backendMock.verifyExpectationsMet();
			assertThat( result )
					.isEqualTo( entityManager.getReference( IndexedEntity.class, 1 ) );
		} );
	}

	@Test
	public void pagination() {
		withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toJpaQuery( createSimpleQuery( searchSession ) );

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
			query.getResultList();
			backendMock.verifyExpectationsMet();
		} );
	}

	@Test
	public void timeout_dsl() {
		withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toJpaQuery(
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
			assertThatThrownBy( () -> query.getResultList() )
					.isInstanceOf( QueryTimeoutException.class )
					.hasCause( timeoutException );
		} );
	}

	@Test
	public void timeout_jpaHint() {
		withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toJpaQuery( createSimpleQuery( searchSession ) );

			query.setHint( "javax.persistence.query.timeout", 200 );

			SearchTimeoutException timeoutException = new SearchTimeoutException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> b.failAfter( 200, TimeUnit.MILLISECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			assertThatThrownBy( () -> query.getResultList() )
					.isInstanceOf( QueryTimeoutException.class )
					.hasCause( timeoutException );
		} );
	}

	@Test
	public void timeout_override() {
		withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toJpaQuery(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.matchAll() )
							.failAfter( 2, TimeUnit.SECONDS )
							.toQuery()
			);

			query.setHint( "javax.persistence.query.timeout", 200 );

			SearchTimeoutException timeoutException = new SearchTimeoutException( "Timed out" );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ),
					b -> b.failAfter( 200, TimeUnit.MILLISECONDS ),
					StubSearchWorkBehavior.failing( () -> timeoutException )
			);

			// Just check that the exception is propagated
			assertThatThrownBy( () -> query.getResultList() )
					.isInstanceOf( QueryTimeoutException.class )
					.hasCause( timeoutException );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graph_jpaHint_fetch() {
		withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.setHint( "javax.persistence.fetchgraph", entityManager.getEntityGraph( IndexedEntity.GRAPH_EAGER ) );

			backendMock.expectSearchObjects(
					IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.getSingleResult();
			assertThatManaged( loaded.getContainedEager() ).isInitialized();
			assertThatManaged( loaded.getContainedLazy() ).isInitialized();
		} );

		withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.setHint( "javax.persistence.fetchgraph", entityManager.getEntityGraph( IndexedEntity.GRAPH_LAZY ) );

			backendMock.expectSearchObjects(
					IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.getResultList().get( 0 );
			// FETCH graph => associations can be forced to lazy even if eager in the mapping
			assertThatManaged( loaded.getContainedEager() ).isNotInitialized();
			assertThatManaged( loaded.getContainedLazy() ).isNotInitialized();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3628")
	public void graph_jpaHint_load() {
		withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.setHint( "javax.persistence.loadgraph", entityManager.getEntityGraph( IndexedEntity.GRAPH_EAGER ) );

			backendMock.expectSearchObjects(
					Arrays.asList( IndexedEntity.NAME ), b -> { },
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.getSingleResult();
			assertThatManaged( loaded.getContainedEager() ).isInitialized();
			assertThatManaged( loaded.getContainedLazy() ).isInitialized();
		} );

		withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toOrmQuery( createSimpleQuery( searchSession ) );

			query.setHint( "javax.persistence.loadgraph", entityManager.getEntityGraph( IndexedEntity.GRAPH_LAZY ) );

			backendMock.expectSearchObjects(
					IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.getSingleResult();
			// LOAD graph => associations cannot be forced to lazy if eager in the mapping
			assertThatManaged( loaded.getContainedEager() ).isInitialized();
			assertThatManaged( loaded.getContainedLazy() ).isNotInitialized();
		} );
	}

	@Test
	public void graph_override_jpaHint() {
		withinEntityManager( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			TypedQuery<IndexedEntity> query = Search.toOrmQuery(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.matchAll() )
							.loading( o -> o.graph( IndexedEntity.GRAPH_EAGER, GraphSemantic.LOAD ) )
							.toQuery()
			);

			query.setHint( "javax.persistence.fetchgraph", entityManager.getEntityGraph( IndexedEntity.GRAPH_LAZY ) );

			backendMock.expectSearchObjects(
					IndexedEntity.NAME,
					StubSearchWorkBehavior.of( 1, reference( IndexedEntity.NAME, "1" ) )
			);

			IndexedEntity loaded = query.getSingleResult();
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
