/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.outbox.impl.OutboxEvent;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.util.common.serialization.spi.SerializationUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.AutomaticIndexingStrategyExpectations;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class OutboxPollingNoProcessingIT {

	private static final String INDEX_NAME = "IndexedEntity";
	private static final String ANOTHER_INDEX_NAME = "AnotherIndexedEntity";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock )
			.automaticIndexingStrategy( AutomaticIndexingStrategyExpectations.outboxPolling() );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema(
				INDEX_NAME,
				b -> b.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);
		backendMock.expectSchema(
				ANOTHER_INDEX_NAME,
				b -> b.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);
		backendMock.expectSchema(
				RoutedIndexedEntity.INDEX_NAME,
				b -> b.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) )
		);

		sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							ProgrammaticMappingConfigurationContext mapping = context.programmaticMapping();
							TypeMappingStep indexedEntityMapping = mapping.type( IndexedEntity.class );
							indexedEntityMapping.indexed().index( INDEX_NAME );
							indexedEntityMapping.property( "id" ).documentId();
							indexedEntityMapping.property( "text" ).fullTextField();

							TypeMappingStep anotherIndexedEntityMapping = mapping.type( AnotherIndexedEntity.class );
							anotherIndexedEntityMapping.indexed().index( ANOTHER_INDEX_NAME );
							anotherIndexedEntityMapping.property( "id" ).documentId();
							anotherIndexedEntityMapping.property( "text" ).fullTextField();

							TypeMappingStep routedIndexedEntityMapping = mapping.type( RoutedIndexedEntity.class );
							routedIndexedEntityMapping.indexed()
									.index( RoutedIndexedEntity.INDEX_NAME )
									.routingBinder( new CustomRoutingBridge.CustomRoutingBinder() );
							routedIndexedEntityMapping.property( "id" ).documentId();
							routedIndexedEntityMapping.property( "text" ).fullTextField();
						}
				)
				.withProperty( "hibernate.search.automatic_indexing.process_outbox_table", "false" )
				.setup( IndexedEntity.class, AnotherIndexedEntity.class, RoutedIndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void insertUpdateDelete() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity indexedPojo = new IndexedEntity( 1, "Using some text here" );
			session.save( indexedPojo );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			List<OutboxEvent> outboxEntries = findOutboxEntries( session );

			assertThat( outboxEntries ).hasSize( 1 );
			verifyOutboxEntry( outboxEntries.get( 0 ), INDEX_NAME, "1", OutboxEvent.Type.ADD, null );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = session.load( IndexedEntity.class, 1 );
			entity.setText( "Change the test of this entity!" );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			List<OutboxEvent> outboxEntries = findOutboxEntries( session );

			assertThat( outboxEntries ).hasSize( 2 );
			verifyOutboxEntry( outboxEntries.get( 1 ), INDEX_NAME, "1", OutboxEvent.Type.ADD_OR_UPDATE, null );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity = session.load( IndexedEntity.class, 1 );
			session.delete( entity );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			List<OutboxEvent> outboxEntries = findOutboxEntries( session );

			assertThat( outboxEntries ).hasSize( 3 );
			verifyOutboxEntry( outboxEntries.get( 2 ), INDEX_NAME, "1", OutboxEvent.Type.DELETE, null );
		} );
	}

	@Test
	public void multipleInstances() {
		for ( int i = 1; i <= 7; i++ ) {
			IndexedEntity indexedPojo = new IndexedEntity( i, "Using some text here" );
			OrmUtils.withinTransaction( sessionFactory, session -> {
				session.save( indexedPojo );
			} );
		}

		OrmUtils.withinTransaction( sessionFactory, session -> {
			List<OutboxEvent> outboxEntries = findOutboxEntries( session );

			assertThat( outboxEntries ).hasSize( 7 );
			for ( int i = 0; i < 7; i++ ) {
				verifyOutboxEntry( outboxEntries.get( i ), INDEX_NAME, ( i + 1 ) + "", OutboxEvent.Type.ADD, null );
			}
		} );
	}

	@Test
	public void multipleTypes() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity indexedPojo = new IndexedEntity( 1, "Using some text here" );
			session.save( indexedPojo );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			AnotherIndexedEntity indexedPojo = new AnotherIndexedEntity( 1, "Using some text here" );
			session.save( indexedPojo );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			List<OutboxEvent> outboxEntries = findOutboxEntries( session );

			assertThat( outboxEntries ).hasSize( 2 );
			verifyOutboxEntry( outboxEntries.get( 0 ), INDEX_NAME, "1", OutboxEvent.Type.ADD, null );
			verifyOutboxEntry( outboxEntries.get( 1 ), ANOTHER_INDEX_NAME, "1", OutboxEvent.Type.ADD, null );
		} );
	}

	@Test
	public void routingKeys() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			RoutedIndexedEntity indexedPojo = new RoutedIndexedEntity( 1, "Using some text here",
					RoutedIndexedEntity.Color.Blue
			);
			session.save( indexedPojo );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			List<OutboxEvent> outboxEntries = findOutboxEntries( session );

			assertThat( outboxEntries ).hasSize( 1 );
			verifyOutboxEntry(
					outboxEntries.get( 0 ), RoutedIndexedEntity.INDEX_NAME, "1", OutboxEvent.Type.ADD, "Blue" );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			RoutedIndexedEntity entity = session.load( RoutedIndexedEntity.class, 1 );
			entity.setText( "Change the test of this entity!" );
			entity.setColor( RoutedIndexedEntity.Color.Red );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			List<OutboxEvent> outboxEntries = findOutboxEntries( session );

			assertThat( outboxEntries ).hasSize( 2 );
			verifyOutboxEntry(
					outboxEntries.get( 1 ), RoutedIndexedEntity.INDEX_NAME, "1", OutboxEvent.Type.ADD_OR_UPDATE, "Red",
					"Blue", "Green", "Yellow", "White" ); // previous routing keys
		} );
	}

	static List<OutboxEvent> findOutboxEntries(Session session) {
		return session.createQuery( "select e from OutboxEvent e order by id", OutboxEvent.class ).list();
	}

	static void verifyOutboxEntry(OutboxEvent outboxEvent, String entityName, String entityId,
			OutboxEvent.Type type, String currentRoute, String... previousRoutes) {
		assertThat( outboxEvent.getEntityName() ).isEqualTo( entityName );
		assertThat( outboxEvent.getEntityId() ).isEqualTo( entityId );
		assertThat( outboxEvent.getType() ).isEqualTo( type );

		byte[] documentRoutes = outboxEvent.getDocumentRoutes();
		DocumentRoutesDescriptor routesDescriptor = SerializationUtils.deserialize(
				DocumentRoutesDescriptor.class, documentRoutes );

		assertThat( routesDescriptor ).isNotNull();
		assertThat( routesDescriptor.currentRoute().routingKey() ).isEqualTo( currentRoute );

		List<DocumentRouteDescriptor> descriptors = Arrays.stream( previousRoutes )
				.map( routingKey -> DocumentRouteDescriptor.of( routingKey ) )
				.collect( Collectors.toList() );

		assertThat( routesDescriptor.previousRoutes() ).isEqualTo( descriptors );
	}

	@Entity(name = INDEX_NAME)
	public static class IndexedEntity {

		@Id
		private Integer id;
		private String text;

		public IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@Entity(name = ANOTHER_INDEX_NAME)
	public static class AnotherIndexedEntity {

		@Id
		private Integer id;
		private String text;

		public AnotherIndexedEntity() {
		}

		public AnotherIndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
