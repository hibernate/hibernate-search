/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.coordination.databasepolling;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.coordination.CoordinationStrategyNames;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.CoordinationStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Extensive tests with edge cases for automatic indexing with {@link CoordinationStrategyNames#DATABASE_POLLING}.
 */
public class DatabasePollingAutomaticIndexingEdgeCasesIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.coordinationStrategy( CoordinationStrategyExpectations.outboxPolling() );

	private final FilteringOutboxEventFinder outboxEventFinder = new FilteringOutboxEventFinder()
			// Disable the filter by default: only some of the tests actually need it.
			.enableFilter( false );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b.field( "indexedField", String.class ) );
		sessionFactory = ormSetupHelper.start()
				.withProperty( "hibernate.search.coordination.processors.indexing.outbox_event_finder.provider", outboxEventFinder.provider() )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void multipleChangesSameTransaction() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue I" );
			session.persist( entity1 );

			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setIndexedField( "initialValue I" );
			session.persist( entity2 );

			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setIndexedField( "initialValue I" );
			session.persist( entity3 );

			entity1.setIndexedField( "initialValue II" );
			session.update( entity1 );
			entity2.setIndexedField( "initialValue II" );
			session.update( entity2 );
			entity3.setIndexedField( "initialValue II" );
			session.update( entity3 );

			entity1.setIndexedField( "initialValue III" );
			session.update( entity1 );
			entity2.setIndexedField( "initialValue III" );
			session.update( entity2 );
			entity3.setIndexedField( "initialValue III" );
			session.update( entity3 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "indexedField", "initialValue III" )
					)
					.add( "2", b -> b
							.field( "indexedField", "initialValue III" )
					)
					.add( "3", b -> b
							.field( "indexedField", "initialValue III" )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void massiveInsert() {
		for ( int i = 0; i < 5; i++ ) {
			int finalI = i;
			OrmUtils.withinTransaction( sessionFactory, session -> {
				BackendMock.DocumentWorkCallListContext context = backendMock.expectWorks( IndexedEntity.INDEX );

				for ( int j = 0; j < 500; j++ ) {
					int index = finalI * 500 + j;

					IndexedEntity entity = new IndexedEntity();
					entity.setId( index );
					entity.setIndexedField( "indexed value: " + index );
					session.persist( entity );

					if ( j % 25 == 0 ) {
						session.flush();
						session.clear();
					}

					context.add( index + "", b -> b.field( "indexedField", "indexed value: " + index ) );
				}
			} );
			backendMock.verifyExpectationsMet();
		}
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String indexedField) {
			this.id = id;
			this.indexedField = indexedField;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}
	}

}
