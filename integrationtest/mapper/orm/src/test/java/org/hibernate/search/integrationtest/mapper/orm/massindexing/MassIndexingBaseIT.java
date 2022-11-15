/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Very basic test to probe an use of {@link MassIndexer} api.
 */
public class MassIndexingBaseIT {

	public static List<? extends Arguments> params() {
		return Arrays.stream( TenancyMode.values() )
				.map( v -> Arguments.of( v ) )
				.collect( Collectors.toList() );
	}

	private static final String TENANT_1_ID = "tenant1";
	private static final String TENANT_2_ID = "tenant2";

	public static final String TITLE_1 = "Oliver Twist";
	public static final String AUTHOR_1 = "Charles Dickens";
	public static final String TITLE_2 = "Ulysses";
	public static final String AUTHOR_2 = "James Joyce";
	public static final String TITLE_3 = "Frankenstein";
	public static final String AUTHOR_3 = "Mary Shelley";
	public static final String TITLE_4 = "The Fellowship Of The Ring";
	public static final String AUTHOR_4 = "J. R. R. Tolkien";

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.createGlobal();

	@RegisterExtension
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock )
			.delayedInitialization( true );

	@RegisterExtension
	public Extension setupHolderMethodRule = setupHolder.methodExtension();

	public TenancyMode tenancyMode;

	@ReusableOrmSetupHolder.SetupParams
	public List<?> setupParams() {
		return Collections.singletonList( tenancyMode );
	}

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext, ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
		backendMock.expectAnySchema( Book.INDEX );

		setupContext.withPropertyRadical( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_ENABLED, false )
				.withAnnotatedTypes( Book.class );

		if ( TenancyMode.MULTI_TENANCY.equals( tenancyMode ) ) {
			setupContext.tenants( TENANT_1_ID, TENANT_2_ID );
			dataClearConfig.tenants( TENANT_1_ID, TENANT_2_ID );
		}
	}

	void init(TenancyMode tenancyMode) {
		this.tenancyMode = tenancyMode;
		setupHolder.initialize();

		initData();
	}

	public void initData() {
		setupHolder.with( targetTenantId() ).runInTransaction( session -> {
			session.persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
			session.persist( new Book( 2, TITLE_2, AUTHOR_2 ) );
			session.persist( new Book( 3, TITLE_3, AUTHOR_3 ) );
		} );

		if ( TenancyMode.MULTI_TENANCY.equals( tenancyMode ) ) {
			// Also add data to the other tenant,
			// in order to trigger failures if the mass indexer does not correctly limit itself
			// to just the target tenant.
			setupHolder.with( TENANT_2_ID ).runInTransaction( session -> {
				session.persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
				session.persist( new Book( 2, TITLE_2, AUTHOR_2 ) );
				session.persist( new Book( 3, TITLE_3, AUTHOR_3 ) );
				session.persist( new Book( 4, TITLE_4, AUTHOR_4 ) );
			} );
		}
	}

	private String targetTenantId() {
		return TenancyMode.MULTI_TENANCY.equals( tenancyMode ) ? TENANT_1_ID : null;
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void defaultMassIndexerStartAndWait(TenancyMode tenancyMode) throws Exception {
		init( tenancyMode );
		setupHolder.with( targetTenantId() ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer();

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Book.INDEX, targetTenantId(), DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> b
							.field( "title", TITLE_1 )
							.field( "author", AUTHOR_1 )
					)
					.add( "2", b -> b
							.field( "title", TITLE_2 )
							.field( "author", AUTHOR_2 )
					)
					.add( "3", b -> b
							.field( "title", TITLE_3 )
							.field( "author", AUTHOR_3 )
					);

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Book.INDEX, targetTenantId() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		backendMock.verifyExpectationsMet();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void dropAndCreateSchemaOnStart(TenancyMode tenancyMode) {
		init( tenancyMode );
		setupHolder.with( targetTenantId() ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer().dropAndCreateSchemaOnStart( true );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Book.INDEX, targetTenantId(), DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> b
							.field( "title", TITLE_1 )
							.field( "author", AUTHOR_1 )
					)
					.add( "2", b -> b
							.field( "title", TITLE_2 )
							.field( "author", AUTHOR_2 )
					)
					.add( "3", b -> b
							.field( "title", TITLE_3 )
							.field( "author", AUTHOR_3 )
					);

			backendMock.expectSchemaManagementWorks( Book.INDEX )
					.work( StubSchemaManagementWork.Type.DROP_AND_CREATE );

			// because we set dropAndCreateSchemaOnStart = true and do not explicitly set the purge value
			// it means that purge will default to false hence only flush and refresh are expected:
			backendMock.expectIndexScaleWorks( Book.INDEX, targetTenantId() )
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		backendMock.verifyExpectationsMet();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void mergeSegmentsOnFinish(TenancyMode tenancyMode) {
		init( tenancyMode );
		setupHolder.with( targetTenantId() ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer().mergeSegmentsOnFinish( true );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Book.INDEX, targetTenantId(), DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
			)
					.add( "1", b -> b
							.field( "title", TITLE_1 )
							.field( "author", AUTHOR_1 )
					)
					.add( "2", b -> b
							.field( "title", TITLE_2 )
							.field( "author", AUTHOR_2 )
					)
					.add( "3", b -> b
							.field( "title", TITLE_3 )
							.field( "author", AUTHOR_3 )
					);

			// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
			// and optimizeOnFinish is enabled explicitly,
			// so we expect 1 purge, 2 optimize and 1 flush calls in this order:
			backendMock.expectIndexScaleWorks( Book.INDEX, targetTenantId() )
					.purge()
					.mergeSegments()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		backendMock.verifyExpectationsMet();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void fromMappingWithoutSession(TenancyMode tenancyMode) throws Exception {
		init( tenancyMode );
		SearchMapping searchMapping = Search.mapping( setupHolder.sessionFactory() );
		MassIndexer indexer = searchMapping.scope( Object.class ).massIndexer( targetTenantId() );

		// add operations on indexes can follow any random order,
		// since they are executed by different threads
		backendMock.expectWorks(
				Book.INDEX, targetTenantId(), DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
		)
				.add( "1", b -> b
						.field( "title", TITLE_1 )
						.field( "author", AUTHOR_1 )
				)
				.add( "2", b -> b
						.field( "title", TITLE_2 )
						.field( "author", AUTHOR_2 )
				)
				.add( "3", b -> b
						.field( "title", TITLE_3 )
						.field( "author", AUTHOR_3 )
				);

		// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
		// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
		backendMock.expectIndexScaleWorks( Book.INDEX, targetTenantId() )
				.purge()
				.mergeSegments()
				.flush()
				.refresh();

		try {
			indexer.startAndWait();
		}
		catch (InterruptedException e) {
			fail( "Unexpected InterruptedException: " + e.getMessage() );
		}

		backendMock.verifyExpectationsMet();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void dropAndCreateSchemaOnStartAndPurgeBothEnabled(TenancyMode tenancyMode) {
		init( tenancyMode );
		setupHolder.with( targetTenantId() ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer()
					.dropAndCreateSchemaOnStart( true )
					.purgeAllOnStart( true );

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
							Book.INDEX, targetTenantId(), DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
					)
					.add( "1", b -> b
							.field( "title", TITLE_1 )
							.field( "author", AUTHOR_1 )
					)
					.add( "2", b -> b
							.field( "title", TITLE_2 )
							.field( "author", AUTHOR_2 )
					)
					.add( "3", b -> b
							.field( "title", TITLE_3 )
							.field( "author", AUTHOR_3 )
					);

			backendMock.expectSchemaManagementWorks( Book.INDEX )
					.work( StubSchemaManagementWork.Type.DROP_AND_CREATE );

			// as purgeAllOnStart is explicitly set to true, and merge is true by default
			// it means that both purge and merge will be triggered:
			backendMock.expectIndexScaleWorks( Book.INDEX, targetTenantId() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		backendMock.verifyExpectationsMet();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void reuseSearchSessionAfterOrmSessionIsClosed_createMassIndexer(TenancyMode tenancyMode) {
		init( tenancyMode );
		Session session = setupHolder.sessionFactory().withOptions()
				.tenantIdentifier( targetTenantId() )
				.openSession();
		SearchSession searchSession = Search.session( session );
		// a SearchSession instance is created lazily,
		// so we need to use it to have an instance of it
		searchSession.massIndexer();
		session.close();

		assertThatThrownBy( () -> {
			searchSession.massIndexer();
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to access Hibernate ORM session", "is closed" );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void lazyCreateSearchSessionAfterOrmSessionIsClosed_createMassIndexer(TenancyMode tenancyMode) {
		init( tenancyMode );
		Session session = setupHolder.sessionFactory().withOptions()
				.tenantIdentifier( targetTenantId() )
				.openSession();
		// Search session is not created, since we don't use it
		SearchSession searchSession = Search.session( session );
		session.close();

		assertThatThrownBy( () -> {
			searchSession.massIndexer();
		} )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to access Hibernate ORM session", "is closed" );
	}

	@Entity
	@Table(name = "book")
	@Indexed(index = Book.INDEX)
	public static class Book {

		public static final String INDEX = "Book";

		@Id
		private Integer id;

		@GenericField
		private String title;

		@GenericField
		private String author;

		public Book() {
		}

		public Book(Integer id, String title, String author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		public Integer getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public String getAuthor() {
			return author;
		}
	}
}
