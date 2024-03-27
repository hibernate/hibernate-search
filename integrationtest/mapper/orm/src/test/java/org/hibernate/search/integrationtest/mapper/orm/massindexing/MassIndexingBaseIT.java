/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;
import static org.hibernate.search.util.common.impl.CollectionHelper.asSet;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
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
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedPerClass;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetup;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetupBeforeTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Very basic test to probe an use of {@link MassIndexer} api.
 */
@ParameterizedPerClass
class MassIndexingBaseIT {

	private SessionFactory sessionFactory;

	public static List<? extends Arguments> params() {
		return Arrays.stream( TenancyMode.values() ).map( Arguments::of ).collect( Collectors.toList() );
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
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	public TenancyMode tenancyMode;

	@ParameterizedSetup
	@MethodSource("params")
	void setup(TenancyMode tenancyMode) {
		this.tenancyMode = tenancyMode;

		backendMock.resetExpectations();
		OrmSetupHelper.SetupContext setupContext = ormSetupHelper.start().withPropertyRadical(
				HibernateOrmMapperSettings.Radicals.INDEXING_LISTENERS_ENABLED, false )
				.withAnnotatedTypes( Book.class );

		if ( TenancyMode.MULTI_TENANCY.equals( tenancyMode ) ) {
			setupContext.tenantsWithHelperEnabled( TENANT_1_ID, TENANT_2_ID );
			setupContext.withProperty(
					HibernateOrmMapperSettings.MULTI_TENANCY_TENANT_IDS,
					String.join( ",", TENANT_1_ID, TENANT_2_ID )
			);
		}

		// We add the schema expectation as a part of a configuration, and as a last configuration.
		// this way we will only set the expectation only when the entire config was a success:
		setupContext.withConfiguration( ignored -> backendMock.expectAnySchema( Book.INDEX ) );
		sessionFactory = setupContext.setup();
	}

	@ParameterizedSetupBeforeTest
	public void init() {
		with( sessionFactory, targetTenantId() ).runInTransaction( session -> {
			session.persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
			session.persist( new Book( 2, TITLE_2, AUTHOR_2 ) );
			session.persist( new Book( 3, TITLE_3, AUTHOR_3 ) );
		} );

		if ( TenancyMode.MULTI_TENANCY.equals( tenancyMode ) ) {
			// Also add data to the other tenant,
			// in order to trigger failures if the mass indexer does not correctly limit itself
			// to just the target tenant.
			with( sessionFactory, TENANT_2_ID ).runInTransaction( session -> {
				session.persist( new Book( 1, TITLE_1, AUTHOR_1 ) );
				session.persist( new Book( 2, TITLE_2, AUTHOR_2 ) );
				session.persist( new Book( 3, TITLE_3, AUTHOR_3 ) );
				session.persist( new Book( 4, TITLE_4, AUTHOR_4 ) );
			} );
		}
	}

	private Object targetTenantId() {
		return TenancyMode.MULTI_TENANCY.equals( tenancyMode ) ? TENANT_1_ID : null;
	}

	private Set<Object> allTenantIds() {
		return TenancyMode.MULTI_TENANCY.equals( tenancyMode ) ? asSet( TENANT_1_ID, TENANT_2_ID ) : Collections.emptySet();
	}

	@Test
	void defaultMassIndexerStartAndWait() throws Exception {
		with( sessionFactory, targetTenantId() ).runNoTransaction( session -> {
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

	@Test
	void dropAndCreateSchemaOnStart() {
		with( sessionFactory, targetTenantId() ).runNoTransaction( session -> {
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

	@Test
	void mergeSegmentsOnFinish() {
		with( sessionFactory, targetTenantId() ).runNoTransaction( session -> {
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

	@Test
	void fromMappingWithoutSession_explicitSingleTenant() {
		SearchMapping searchMapping = Search.mapping( sessionFactory );
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

	@Test
	void fromMappingWithoutSession_explicitAllTenants() {
		SearchMapping searchMapping = Search.mapping( sessionFactory );
		MassIndexer indexer = searchMapping.scope( Object.class ).massIndexer( allTenantIds() );

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
		if ( TenancyMode.MULTI_TENANCY.equals( tenancyMode ) ) {
			backendMock.expectWorks(
					Book.INDEX, TENANT_2_ID, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
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
					)
					.add( "4", b -> b
							.field( "title", TITLE_4 )
							.field( "author", AUTHOR_4 )
					);
		}

		// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
		// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
		// But if we are in multi-tenant case then we expect that purge will be called for all tenants first,
		// while other work like merge or flush/refresh will be called just once for the first tenant in the list:

		if ( TenancyMode.MULTI_TENANCY.equals( tenancyMode ) ) {
			backendMock.expectIndexScaleWorks( Book.INDEX, targetTenantId(), TENANT_2_ID )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
		}
		else {
			backendMock.expectIndexScaleWorks( Book.INDEX, targetTenantId() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
		}

		try {
			indexer.startAndWait();
		}
		catch (InterruptedException e) {
			fail( "Unexpected InterruptedException: " + e.getMessage() );
		}

		backendMock.verifyExpectationsMet();
	}

	@Test
	void fromMappingWithoutSession_implicitTenant() {
		SearchMapping searchMapping = Search.mapping( sessionFactory );
		// We don't pass a tenant ID, it's implicit:
		// in single-tenancy mode the tenant ID is set to `null`,
		// and in multi-tenancy mode the tenant IDs are extracted from the configuration properties.
		MassIndexer indexer = searchMapping.scope( Object.class ).massIndexer();

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
		if ( TenancyMode.MULTI_TENANCY.equals( tenancyMode ) ) {
			backendMock.expectWorks(
					Book.INDEX, TENANT_2_ID, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
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
					)
					.add( "4", b -> b
							.field( "title", TITLE_4 )
							.field( "author", AUTHOR_4 )
					);
		}

		// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
		// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
		// But if we are in multi-tenant case then we expect that purge will be called for all tenants first,
		// while other work like merge or flush/refresh will be called just once for the first tenant in the list:
		if ( TenancyMode.MULTI_TENANCY.equals( tenancyMode ) ) {
			backendMock.expectIndexScaleWorks( Book.INDEX, targetTenantId(), TENANT_2_ID )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
		}
		else {
			backendMock.expectIndexScaleWorks( Book.INDEX, targetTenantId() )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();
		}


		try {
			indexer.startAndWait();
		}
		catch (InterruptedException e) {
			fail( "Unexpected InterruptedException: " + e.getMessage() );
		}

		backendMock.verifyExpectationsMet();
	}

	@Test
	void dropAndCreateSchemaOnStartAndPurgeBothEnabled() {
		with( sessionFactory, targetTenantId() ).runNoTransaction( session -> {
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

	@Test
	void reuseSearchSessionAfterOrmSessionIsClosed_createMassIndexer() {
		Session session = sessionFactory.withOptions()
				.tenantIdentifier( (Object) targetTenantId() )
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

	@Test
	void lazyCreateSearchSessionAfterOrmSessionIsClosed_createMassIndexer() {
		Session session = sessionFactory.withOptions()
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
