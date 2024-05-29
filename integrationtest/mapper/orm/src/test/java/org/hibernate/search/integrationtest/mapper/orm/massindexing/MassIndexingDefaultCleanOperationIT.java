/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.massindexing;

import static org.assertj.core.api.Fail.fail;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.tenancy.spi.TenancyMode;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingDefaultCleanOperation;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MassIndexingDefaultCleanOperationIT {

	public static List<? extends Arguments> params() {
		List<Arguments> params = new ArrayList<>();

		for ( MassIndexingDefaultCleanOperation operation : MassIndexingDefaultCleanOperation.values() ) {
			for ( TenancyMode tenancyMode : TenancyMode.values() ) {
				params.add( Arguments.of( operation, tenancyMode ) );
			}
		}

		return params;
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

	@ParameterizedTest
	@MethodSource("params")
	void test(MassIndexingDefaultCleanOperation operation, TenancyMode tenancyMode) {
		assumeFalse(
				TenancyMode.MULTI_TENANCY.equals( tenancyMode )
						&& MassIndexingDefaultCleanOperation.DROP_AND_CREATE.equals( operation ),
				"Drop and create schema only on makes sense if there's no multi-tenancy. "
						+ "Otherwise mass indexer is created for a single tenant and schema cannot be dropped resulting in an exception" );

		backendMock.resetExpectations();

		OrmSetupHelper.SetupContext setupContext = ormSetupHelper.start()
				.withPropertyRadical( HibernateOrmMapperSettings.Radicals.INDEXING_LISTENERS_ENABLED, false )
				.withPropertyRadical( HibernateOrmMapperSettings.Radicals.INDEXING_MASS_DEFAULT_CLEAN_OPERATION, operation )
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
		SessionFactory sessionFactory = setupContext.setup();

		with( sessionFactory, targetTenantId( tenancyMode ) ).runInTransaction( session -> {
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


		with( sessionFactory, targetTenantId( tenancyMode ) ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			MassIndexer indexer = searchSession.massIndexer();

			// add operations on indexes can follow any random order,
			// since they are executed by different threads
			backendMock.expectWorks(
					Book.INDEX, targetTenantId( tenancyMode ), DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE
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

			if ( MassIndexingDefaultCleanOperation.PURGE.equals( operation ) ) {
				// purgeAtStart and mergeSegmentsAfterPurge are enabled by default,
				// so we expect 1 purge, 1 mergeSegments and 1 flush calls in this order:
				backendMock.expectIndexScaleWorks( Book.INDEX, targetTenantId( tenancyMode ) )
						.purge()
						.mergeSegments()
						.flush()
						.refresh();
			}
			else {
				backendMock.expectSchemaManagementWorks( Book.INDEX )
						.work( StubSchemaManagementWork.Type.DROP_AND_CREATE );

				backendMock.expectIndexScaleWorks( Book.INDEX, targetTenantId( tenancyMode ) )
						.flush()
						.refresh();
			}

			try {
				indexer.startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected InterruptedException: " + e.getMessage() );
			}

		} );

		backendMock.verifyExpectationsMet();
	}

	private Object targetTenantId(TenancyMode tenancyMode) {
		return TenancyMode.MULTI_TENANCY.equals( tenancyMode ) ? TENANT_1_ID : null;
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
