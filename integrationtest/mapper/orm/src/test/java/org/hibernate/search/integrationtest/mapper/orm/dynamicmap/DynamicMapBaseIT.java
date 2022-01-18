/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.dynamicmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test basic features when mapping a Hibernate ORM "dynamic-map" entity.
 * <p>
 * This test is rather simplistic because "dynamic-map" entity mapping is not fully supported in Hibernate Search yet.
 */
@SuppressWarnings("rawtypes")
public class DynamicMapBaseIT {

	private static final String INDEX1_NAME = "Index1Name";

	private static final String INDEX2_NAME = "Index2Name";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	public void simple() {
		String hbmPath = "/DynamicMapBaseIT/simple.hbm.xml";
		String entityTypeName = "Book";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "title", String.class, b2 -> b2.analyzerName( "myAnalyzer" ) )
				.field( "pageCount", Integer.class )
				.field( "publicationDate", LocalDate.class )
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep typeMapping = context.programmaticMapping().type( entityTypeName );
							typeMapping.indexed().index( INDEX1_NAME );
							typeMapping.property( "title" ).fullTextField().analyzer( "myAnalyzer" );
							typeMapping.property( "pageCount" ).genericField();
							typeMapping.property( "publicationDate" ).genericField();
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Map<String, Object> entity1 = new HashMap<>();
			entity1.put( "id", 1 );
			entity1.put( "title", "Hyperion" );
			entity1.put( "pageCount", 321 );
			entity1.put( "publicationDate", LocalDate.of( 1972, Month.MARCH, 3 ) );

			session.persist( entityTypeName, entity1 );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "1", b -> b
							.field( "title", entity1.get( "title" ) )
							.field( "pageCount", entity1.get( "pageCount" ) )
							.field( "publicationDate", entity1.get( "publicationDate" ) )
					);
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<Map> query = searchSession.search(
					searchSession.scope( Map.class, entityTypeName )
			)
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( INDEX1_NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							1L,
							reference( entityTypeName, "1" )
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					(Map) session.getReference( entityTypeName, 1 )
			);
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4656")
	public void typeName_invalid() {
		String hbmPath = "/DynamicMapBaseIT/simple.hbm.xml";
		String entityTypeName = "Book";

		assertThatThrownBy( () -> ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							context.programmaticMapping().type( "invalid" );
						}
				)
				.setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unknown type: 'invalid'",
						"Available named types: [" + entityTypeName + "]",
						"For entity types, the correct type name is the entity name",
						"For component types (embeddeds, ...) in dynamic-map entities,"
								+ " the correct type name is name of the owner entity followed by a dot ('.')"
								+ " followed by the dot-separated path to the component",
						"e.g. 'MyEntity.myEmbedded' or 'MyEntity.myEmbedded.myNestedEmbedded'." );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3848")
	public void searchObject() {
		String hbmPath = "/DynamicMapBaseIT/simple.hbm.xml";
		String entityTypeName = "Book";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "title", String.class )
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep typeMapping = context.programmaticMapping().type( entityTypeName );
							typeMapping.indexed().index( INDEX1_NAME );
							typeMapping.property( "title" ).keywordField();
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Map<String, Object> entity1 = new HashMap<>();
			entity1.put( "id", 1 );
			entity1.put( "title", "Hyperion" );

			session.persist( entityTypeName, entity1 );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "1", b -> b
							.field( "title", entity1.get( "title" ) )
					);
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<Object> query = searchSession.search(
					searchSession.scope( Object.class )
			)
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( INDEX1_NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							1L,
							reference( entityTypeName, "1" )
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					session.getReference( entityTypeName, 1 )
			);
		} );
	}

	@Test
	public void massIndexing() {
		String hbmPath = "/DynamicMapBaseIT/simple.hbm.xml";
		String entityTypeName = "Book";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "title", String.class )
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLED, false )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep typeMapping = context.programmaticMapping().type( entityTypeName );
							typeMapping.indexed().index( INDEX1_NAME );
							typeMapping.property( "title" ).genericField();
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			for ( int i = 0; i < 100; i++ ) {
				Map<String, Object> entity = new HashMap<>();
				entity.put( "id", i );
				entity.put( "title", "Hyperion " + i );

				session.persist( entityTypeName, entity );
			}
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchScope<Map> scope = searchSession.scope( Map.class, entityTypeName );

			backendMock.expectIndexScaleWorks( INDEX1_NAME )
					.purge()
					.mergeSegments()
					.flush()
					.refresh();

			for ( int i = 0; i < 100; i++ ) {
				int id = i;
				backendMock.expectWorks( INDEX1_NAME, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE )
						.add( String.valueOf( id ), b -> b.field( "title","Hyperion " + id ) );
			}

			try {
				scope.massIndexer().startAndWait();
			}
			catch (InterruptedException e) {
				fail( "Unexpected exception", e );
			}
		} );
	}

	@Test
	public void nonEntityIdDocumentId() {
		String hbmPath = "/DynamicMapBaseIT/simple.hbm.xml";
		String entityTypeName = "Book";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "title", String.class )
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep typeMapping = context.programmaticMapping().type( entityTypeName );
							typeMapping.indexed().index( INDEX1_NAME );
							typeMapping.property( "title" ).documentId()
									.genericField();
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Map<String, Object> entity1 = new HashMap<>();
			entity1.put( "id", 1 );
			entity1.put( "title", "Hyperion" );

			session.persist( entityTypeName, entity1 );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "Hyperion", b -> b
							.field( "title", entity1.get( "title" ) )
					);
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<Object> query = searchSession.search(
					searchSession.scope( Object.class )
			)
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( INDEX1_NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							1L,
							reference( entityTypeName, "Hyperion" )
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					session.getReference( entityTypeName, 1 )
			);
		} );
	}

	@Test
	public void inheritance() {
		String hbmPath = "/DynamicMapBaseIT/inheritance.hbm.xml";
		String entityATypeName = "A";
		String entityA_BTypeName = "A_B";
		String entityA_CTypeName = "A_C";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "propertyOfA", String.class, b2 -> b2.analyzerName( "myAnalyzer" ) )
				.field( "propertyOfB", Integer.class )
		);
		backendMock.expectSchema( INDEX2_NAME, b -> b
				.field( "propertyOfA", String.class, b2 -> b2.analyzerName( "myAnalyzer" ) )
				.field( "propertyOfC", LocalDate.class )
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep entityATypeMapping = context.programmaticMapping().type( entityATypeName );
							entityATypeMapping.property( "propertyOfA" ).fullTextField().analyzer( "myAnalyzer" );

							TypeMappingStep entityA_BTypeMapping = context.programmaticMapping().type( entityA_BTypeName );
							entityA_BTypeMapping.indexed().index( INDEX1_NAME );
							entityA_BTypeMapping.property( "propertyOfB" ).genericField();

							TypeMappingStep entityA_CTypeMapping = context.programmaticMapping().type( entityA_CTypeName );
							entityA_CTypeMapping.indexed().index( INDEX2_NAME );
							entityA_CTypeMapping.property( "propertyOfC" ).genericField();
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Map<String, Object> entity1 = new HashMap<>();
			entity1.put( "id", 1 );
			entity1.put( "propertyOfA", "Hyperion" );
			entity1.put( "propertyOfB", 321 );

			Map<String, Object> entity2 = new HashMap<>();
			entity2.put( "id", 2 );
			entity2.put( "propertyOfA", "Ultron" );
			entity2.put( "propertyOfC", LocalDate.of( 1975, Month.MARCH, 15 ) );

			session.persist( entityA_BTypeName, entity1 );
			session.persist( entityA_CTypeName, entity2 );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "1", b -> b
							.field( "propertyOfA", entity1.get( "propertyOfA" ) )
							.field( "propertyOfB", entity1.get( "propertyOfB" ) )
					);
			backendMock.expectWorks( INDEX2_NAME )
					.add( "2", b -> b
							.field( "propertyOfA", entity2.get( "propertyOfA" ) )
							.field( "propertyOfC", entity2.get( "propertyOfC" ) )
					);
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runNoTransaction( session -> {
			SearchSession searchSession = Search.session( session );

			SearchQuery<Map> query = searchSession.search(
					searchSession.scope( Map.class, Arrays.asList( entityA_BTypeName, entityA_CTypeName ) )
			)
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects(
					Arrays.asList( INDEX1_NAME, INDEX2_NAME ),
					b -> { },
					StubSearchWorkBehavior.of(
							2L,
							reference( entityA_BTypeName, "1" ),
							reference( entityA_CTypeName, "2" )
					)
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					(Map) session.getReference( entityA_BTypeName, 1 ),
					(Map) session.getReference( entityA_CTypeName, 2 )
			);
		} );
	}

	@Test
	public void embedded_dynamicMap() {
		String hbmPath = "/DynamicMapBaseIT/embedded_dynamicmap.hbm.xml";
		String bookTypeName = "Book";
		String quoteTypeName = "Book.quote";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "title", String.class, b2 -> b2.analyzerName( "myAnalyzer" ) )
				.objectField( "quote", b2 -> b2
						.field( "author", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
						.field( "content", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
				)
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep bookTypeMapping = context.programmaticMapping().type( bookTypeName );
							bookTypeMapping.indexed().index( INDEX1_NAME );
							bookTypeMapping.property( "title" ).fullTextField().analyzer( "myAnalyzer" );
							bookTypeMapping.property( "quote" ).indexedEmbedded();

							TypeMappingStep quoteTypeMapping = context.programmaticMapping().type( quoteTypeName );
							quoteTypeMapping.property( "author" ).fullTextField().analyzer( "myAnalyzer" );
							quoteTypeMapping.property( "content" ).fullTextField().analyzer( "myAnalyzer" );
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Map<String, Object> book = new HashMap<>();
			book.put( "id", 1 );
			book.put( "title", "Hyperion" );

			Map<String, Object> quote1 = new HashMap<>();
			quote1.put( "author", "The New York Times Book Review" );
			quote1.put( "content", "An unfailingly inventive narrative" );

			book.put( "quote", quote1 );

			session.persist( bookTypeName, book );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "1", b -> b
							.field( "title", book.get( "title" ) )
							.objectField( "quote", b2 -> b2
									.field( "author", quote1.get( "author" ) )
									.field( "content", quote1.get( "content" ) )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void embedded_class() {
		String hbmPath = "/DynamicMapBaseIT/embedded_class.hbm.xml";
		String bookTypeName = "Book";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "title", String.class, b2 -> b2.analyzerName( "myAnalyzer" ) )
				.objectField( "quote", b2 -> b2
						.field( "author", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
						.field( "content", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
				)
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep bookTypeMapping = context.programmaticMapping().type( bookTypeName );
							bookTypeMapping.indexed().index( INDEX1_NAME );
							bookTypeMapping.property( "title" ).fullTextField().analyzer( "myAnalyzer" );
							bookTypeMapping.property( "quote" ).indexedEmbedded();

							TypeMappingStep quoteTypeMapping = context.programmaticMapping().type( QuoteEmbeddable.class );
							quoteTypeMapping.property( "author" ).fullTextField().analyzer( "myAnalyzer" );
							quoteTypeMapping.property( "content" ).fullTextField().analyzer( "myAnalyzer" );
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Map<String, Object> book = new HashMap<>();
			book.put( "id", 1 );
			book.put( "title", "Hyperion" );

			QuoteEmbeddable quote1 = new QuoteEmbeddable();
			quote1.author = "The New York Times Book Review";
			quote1.content = "An unfailingly inventive narrative";

			book.put( "quote", quote1 );

			session.persist( bookTypeName, book );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "1", b -> b
							.field( "title", book.get( "title" ) )
							.objectField( "quote", b2 -> b2
									.field( "author", quote1.author )
									.field( "content", quote1.content )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void embedded_class_list() {
		String hbmPath = "/DynamicMapBaseIT/embedded_class_list.hbm.xml";
		String bookTypeName = "Book";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "title", String.class, b2 -> b2.analyzerName( "myAnalyzer" ) )
				.objectField( "quotes", b2 -> b2
						.multiValued( true )
						.field( "author", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
						.field( "content", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
				)
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep bookTypeMapping = context.programmaticMapping().type( bookTypeName );
							bookTypeMapping.indexed().index( INDEX1_NAME );
							bookTypeMapping.property( "title" ).fullTextField().analyzer( "myAnalyzer" );
							bookTypeMapping.property( "quotes" ).indexedEmbedded();

							TypeMappingStep quoteTypeMapping = context.programmaticMapping().type( QuoteEmbeddable.class );
							quoteTypeMapping.property( "author" ).fullTextField().analyzer( "myAnalyzer" );
							quoteTypeMapping.property( "content" ).fullTextField().analyzer( "myAnalyzer" );
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Map<String, Object> book = new HashMap<>();
			book.put( "id", 1 );
			book.put( "title", "Hyperion" );

			QuoteEmbeddable quote1 = new QuoteEmbeddable();
			quote1.author = "The New York Times Book Review";
			quote1.content = "An unfailingly inventive narrative";

			QuoteEmbeddable quote2 = new QuoteEmbeddable();
			quote2.author = "The Denver Post";
			quote2.content = "Simmons's own genius transforms space opera into a new kind of poetry";

			ArrayList<QuoteEmbeddable> quotes = new ArrayList<>();
			quotes.add( quote1 );
			quotes.add( quote2 );
			book.put( "quotes", quotes );

			session.persist( bookTypeName, book );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "1", b -> b
							.field( "title", book.get( "title" ) )
							.objectField( "quotes", b2 -> b2
									.field( "author", quote1.author )
									.field( "content", quote1.content )
							)
							.objectField( "quotes", b2 -> b2
									.field( "author", quote2.author )
									.field( "content", quote2.content )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void basic_list() {
		String hbmPath = "/DynamicMapBaseIT/basic_list.hbm.xml";
		String bookTypeName = "Book";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "title", String.class, b2 -> b2.analyzerName( "myAnalyzer" ) )
				.field( "quotes", String.class, b2 -> b2.multiValued( true ).analyzerName( "myAnalyzer" ) )
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep bookTypeMapping = context.programmaticMapping().type( bookTypeName );
							bookTypeMapping.indexed().index( INDEX1_NAME );
							bookTypeMapping.property( "title" ).fullTextField().analyzer( "myAnalyzer" );
							bookTypeMapping.property( "quotes" ).fullTextField().analyzer( "myAnalyzer" );
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Map<String, Object> book = new HashMap<>();
			book.put( "id", 1 );
			book.put( "title", "Hyperion" );

			String quote1 = "An unfailingly inventive narrative";
			String quote2 = "Simmons's own genius transforms space opera into a new kind of poetry";

			ArrayList<String> quotes = new ArrayList<>();
			quotes.add( quote1 );
			quotes.add( quote2 );
			book.put( "quotes", quotes );

			session.persist( bookTypeName, book );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "1", b -> b
							.field( "title", book.get( "title" ) )
							.field( "quotes", quote1 , quote2 )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@Ignore("toone associations are buggy in dynamic-map mode -- see https://hibernate.atlassian.net/browse/HHH-16100")
	public void toOne() {
		String hbmPath = "/DynamicMapBaseIT/toone.hbm.xml";
		String bookTypeName = "Book";
		String quoteTypeName = "Quote";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "title", String.class, b2 -> b2.analyzerName( "myAnalyzer" ) )
				.objectField( "quote", b2 -> b2
						.field( "author", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
						.field( "content", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
				)
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep bookTypeMapping = context.programmaticMapping().type( bookTypeName );
							bookTypeMapping.indexed().index( INDEX1_NAME );
							bookTypeMapping.property( "title" ).fullTextField().analyzer( "myAnalyzer" );
							bookTypeMapping.property( "quote" ).indexedEmbedded();

							TypeMappingStep quoteTypeMapping = context.programmaticMapping().type( quoteTypeName );
							quoteTypeMapping.property( "author" ).fullTextField().analyzer( "myAnalyzer" );
							quoteTypeMapping.property( "content" ).fullTextField().analyzer( "myAnalyzer" );
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Map<String, Object> book = new HashMap<>();
			book.put( "id", 1 );
			book.put( "title", "Hyperion" );

			Map<String, Object> quote1 = new HashMap<>();
			quote1.put( "id", 2 );
			quote1.put( "author", "The New York Times Book Review" );
			quote1.put( "content", "An unfailingly inventive narrative" );

			book.put( "quote", quote1 );
			quote1.put( "book", book );

			session.persist( bookTypeName, book );
			session.persist( quoteTypeName, quote1 );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "1", b -> b
							.field( "title", book.get( "title" ) )
							.objectField( "quote", b2 -> b2
									.field( "author", quote1.get( "author" ) )
									.field( "content", quote1.get( "content" ) )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void onetomany_bag() {
		String hbmPath = "/DynamicMapBaseIT/onetomany_bag.hbm.xml";
		String bookTypeName = "Book";
		String quoteTypeName = "Quote";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "title", String.class, b2 -> b2.analyzerName( "myAnalyzer" ) )
				.objectField( "quotes", b2 -> b2
						.multiValued( true )
						.field( "author", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
						.field( "content", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
				)
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep bookTypeMapping = context.programmaticMapping().type( bookTypeName );
							bookTypeMapping.indexed().index( INDEX1_NAME );
							bookTypeMapping.property( "title" ).fullTextField().analyzer( "myAnalyzer" );
							bookTypeMapping.property( "quotes" )
									.indexedEmbedded()
									// Necessary because there's no concept of "mappedBy" in hbm.xml.
									.associationInverseSide( PojoModelPath.ofValue( "book" ) );

							TypeMappingStep quoteTypeMapping = context.programmaticMapping().type( quoteTypeName );
							quoteTypeMapping.property( "author" ).fullTextField().analyzer( "myAnalyzer" );
							quoteTypeMapping.property( "content" ).fullTextField().analyzer( "myAnalyzer" );
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Map<String, Object> book = new HashMap<>();
			book.put( "id", 1 );
			book.put( "title", "Hyperion" );

			Map<String, Object> quote1 = new HashMap<>();
			quote1.put( "id", 2 );
			quote1.put( "author", "The New York Times Book Review" );
			quote1.put( "content", "An unfailingly inventive narrative" );

			Map<String, Object> quote2 = new HashMap<>();
			quote2.put( "id", 3 );
			quote2.put( "author", "The Denver Post" );
			quote2.put( "content", "Simmons's own genius transforms space opera into a new kind of poetry" );

			List<Map<String, Object>> quotes = new ArrayList<>();
			quote1.put( "book", book );
			quotes.add( quote1 );
			quote2.put( "book", book );
			quotes.add( quote2 );
			book.put( "quotes", quotes );

			session.persist( bookTypeName, book );
			session.persist( quoteTypeName, quote1 );
			session.persist( quoteTypeName, quote2 );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "1", b -> b
							.field( "title", book.get( "title" ) )
							.objectField( "quotes", b2 -> b2
									.field( "author", quote1.get( "author" ) )
									.field( "content", quote1.get( "content" ) )
							)
							.objectField( "quotes", b2 -> b2
									.field( "author", quote2.get( "author" ) )
									.field( "content", quote2.get( "content" ) )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void onetomany_list() {
		String hbmPath = "/DynamicMapBaseIT/onetomany_list.hbm.xml";
		String bookTypeName = "Book";
		String quoteTypeName = "Quote";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "title", String.class, b2 -> b2.analyzerName( "myAnalyzer" ) )
				.objectField( "quotes", b2 -> b2
						.multiValued( true )
						.field( "author", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
						.field( "content", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
				)
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep bookTypeMapping = context.programmaticMapping().type( bookTypeName );
							bookTypeMapping.indexed().index( INDEX1_NAME );
							bookTypeMapping.property( "title" ).fullTextField().analyzer( "myAnalyzer" );
							bookTypeMapping.property( "quotes" )
									.indexedEmbedded()
									// Necessary because there's no concept of "mappedBy" in hbm.xml.
									.associationInverseSide( PojoModelPath.ofValue( "book" ) );

							TypeMappingStep quoteTypeMapping = context.programmaticMapping().type( quoteTypeName );
							quoteTypeMapping.property( "author" ).fullTextField().analyzer( "myAnalyzer" );
							quoteTypeMapping.property( "content" ).fullTextField().analyzer( "myAnalyzer" );
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Map<String, Object> book = new HashMap<>();
			book.put( "id", 1 );
			book.put( "title", "Hyperion" );

			Map<String, Object> quote1 = new HashMap<>();
			quote1.put( "id", 2 );
			quote1.put( "author", "The New York Times Book Review" );
			quote1.put( "content", "An unfailingly inventive narrative" );

			Map<String, Object> quote2 = new HashMap<>();
			quote2.put( "id", 3 );
			quote2.put( "author", "The Denver Post" );
			quote2.put( "content", "Simmons's own genius transforms space opera into a new kind of poetry" );

			List<Map<String, Object>> quotes = new ArrayList<>();
			quote1.put( "book", book );
			quotes.add( quote1 );
			quote2.put( "book", book );
			quotes.add( quote2 );
			book.put( "quotes", quotes );

			session.persist( bookTypeName, book );
			session.persist( quoteTypeName, quote1 );
			session.persist( quoteTypeName, quote2 );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "1", b -> b
							.field( "title", book.get( "title" ) )
							.objectField( "quotes", b2 -> b2
									.field( "author", quote1.get( "author" ) )
									.field( "content", quote1.get( "content" ) )
							)
							.objectField( "quotes", b2 -> b2
									.field( "author", quote2.get( "author" ) )
									.field( "content", quote2.get( "content" ) )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void onetomany_set() {
		String hbmPath = "/DynamicMapBaseIT/onetomany_set.hbm.xml";
		String bookTypeName = "Book";
		String quoteTypeName = "Quote";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "title", String.class, b2 -> b2.analyzerName( "myAnalyzer" ) )
				.objectField( "quotes", b2 -> b2
						.multiValued( true )
						.field( "author", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
						.field( "content", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
				)
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep bookTypeMapping = context.programmaticMapping().type( bookTypeName );
							bookTypeMapping.indexed().index( INDEX1_NAME );
							bookTypeMapping.property( "title" ).fullTextField().analyzer( "myAnalyzer" );
							bookTypeMapping.property( "quotes" )
									.indexedEmbedded()
									// Necessary because there's no concept of "mappedBy" in hbm.xml.
									.associationInverseSide( PojoModelPath.ofValue( "book" ) );

							TypeMappingStep quoteTypeMapping = context.programmaticMapping().type( quoteTypeName );
							quoteTypeMapping.property( "author" ).fullTextField().analyzer( "myAnalyzer" );
							quoteTypeMapping.property( "content" ).fullTextField().analyzer( "myAnalyzer" );
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Map<String, Object> book = new SafeHashCodeDynamicEntity();
			book.put( "id", 1 );
			book.put( "title", "Hyperion" );

			Map<String, Object> quote1 = new SafeHashCodeDynamicEntity();
			quote1.put( "id", 2 );
			quote1.put( "author", "The New York Times Book Review" );
			quote1.put( "content", "An unfailingly inventive narrative" );

			Map<String, Object> quote2 = new SafeHashCodeDynamicEntity();
			quote2.put( "id", 3 );
			quote2.put( "author", "The Denver Post" );
			quote2.put( "content", "Simmons's own genius transforms space opera into a new kind of poetry" );

			Set<Map<String, Object>> quotes = new TreeSet<>( new DynamicMapIdComparator() );
			quote1.put( "book", book );
			quotes.add( quote1 );
			quote2.put( "book", book );
			quotes.add( quote2 );
			book.put( "quotes", quotes );

			session.persist( bookTypeName, book );
			session.persist( quoteTypeName, quote1 );
			session.persist( quoteTypeName, quote2 );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "1", b -> b
							.field( "title", book.get( "title" ) )
							.objectField( "quotes", b2 -> b2
									.field( "author", quote1.get( "author" ) )
									.field( "content", quote1.get( "content" ) )
							)
							.objectField( "quotes", b2 -> b2
									.field( "author", quote2.get( "author" ) )
									.field( "content", quote2.get( "content" ) )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void onetomany_map_key() {
		String hbmPath = "/DynamicMapBaseIT/onetomany_map_key.hbm.xml";
		String bookTypeName = "Book";
		String quoteTypeName = "Quote";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "title", String.class, b2 -> b2.analyzerName( "myAnalyzer" ) )
				.objectField( "quotes", b2 -> b2
						.multiValued( true )
						.field( "author", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
						.field( "content", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
				)
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep bookTypeMapping = context.programmaticMapping().type( bookTypeName );
							bookTypeMapping.indexed().index( INDEX1_NAME );
							bookTypeMapping.property( "title" ).fullTextField().analyzer( "myAnalyzer" );
							bookTypeMapping.property( "quotes" )
									.indexedEmbedded()
											.extractor( BuiltinContainerExtractors.MAP_KEY )
									// Necessary because there's no concept of "mappedBy" in hbm.xml.
									.associationInverseSide( PojoModelPath.ofValue( "book" ) )
											.extractor( BuiltinContainerExtractors.MAP_KEY );

							TypeMappingStep quoteTypeMapping = context.programmaticMapping().type( quoteTypeName );
							quoteTypeMapping.property( "author" ).fullTextField().analyzer( "myAnalyzer" );
							quoteTypeMapping.property( "content" ).fullTextField().analyzer( "myAnalyzer" );
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Map<String, Object> book = new SafeHashCodeDynamicEntity();
			book.put( "id", 1 );
			book.put( "title", "Hyperion" );

			Map<String, Object> quote1 = new SafeHashCodeDynamicEntity();
			quote1.put( "id", 2 );
			quote1.put( "author", "The New York Times Book Review" );
			quote1.put( "content", "An unfailingly inventive narrative" );

			Map<String, Object> quote2 = new SafeHashCodeDynamicEntity();
			quote2.put( "id", 3 );
			quote2.put( "author", "The Denver Post" );
			quote2.put( "content", "Simmons's own genius transforms space opera into a new kind of poetry" );

			Map<Map<String, Object>, Integer> quotes = new TreeMap<>( new DynamicMapIdComparator() );
			quote1.put( "book", book );
			quotes.put( quote1, 4 );
			quote2.put( "book", book );
			quotes.put( quote2, 5 );
			book.put( "quotes", quotes );

			session.persist( bookTypeName, book );
			session.persist( quoteTypeName, quote1 );
			session.persist( quoteTypeName, quote2 );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "1", b -> b
							.field( "title", book.get( "title" ) )
							.objectField( "quotes", b2 -> b2
									.field( "author", quote1.get( "author" ) )
									.field( "content", quote1.get( "content" ) )
							)
							.objectField( "quotes", b2 -> b2
									.field( "author", quote2.get( "author" ) )
									.field( "content", quote2.get( "content" ) )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void onetomany_map_value() {
		String hbmPath = "/DynamicMapBaseIT/onetomany_map_value.hbm.xml";
		String bookTypeName = "Book";
		String quoteTypeName = "Quote";

		backendMock.expectSchema( INDEX1_NAME, b -> b
				.field( "title", String.class, b2 -> b2.analyzerName( "myAnalyzer" ) )
				.objectField( "quotes", b2 -> b2
						.multiValued( true )
						.field( "author", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
						.field( "content", String.class, b3 -> b3.analyzerName( "myAnalyzer" ) )
				)
		);
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withConfiguration( builder -> builder.addHbmFromClassPath( hbmPath ) )
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							TypeMappingStep bookTypeMapping = context.programmaticMapping().type( bookTypeName );
							bookTypeMapping.indexed().index( INDEX1_NAME );
							bookTypeMapping.property( "title" ).fullTextField().analyzer( "myAnalyzer" );
							bookTypeMapping.property( "quotes" )
									.indexedEmbedded()
									// Necessary because there's no concept of "mappedBy" in hbm.xml.
									.associationInverseSide( PojoModelPath.ofValue( "book" ) );

							TypeMappingStep quoteTypeMapping = context.programmaticMapping().type( quoteTypeName );
							quoteTypeMapping.property( "author" ).fullTextField().analyzer( "myAnalyzer" );
							quoteTypeMapping.property( "content" ).fullTextField().analyzer( "myAnalyzer" );
						}
				)
				.setup();
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			Map<String, Object> book = new HashMap<>();
			book.put( "id", 1 );
			book.put( "title", "Hyperion" );

			Map<String, Object> quote1 = new HashMap<>();
			quote1.put( "id", 2 );
			quote1.put( "author", "The New York Times Book Review" );
			quote1.put( "content", "An unfailingly inventive narrative" );

			Map<String, Object> quote2 = new HashMap<>();
			quote2.put( "id", 3 );
			quote2.put( "author", "The Denver Post" );
			quote2.put( "content", "Simmons's own genius transforms space opera into a new kind of poetry" );

			Map<String, Map<String, Object>> quotes = new LinkedHashMap<>();
			quote1.put( "book", book );
			quotes.put( (String) quote1.get( "author" ), quote1 );
			quote2.put( "book", book );
			quotes.put( (String) quote2.get( "author" ), quote2 );
			book.put( "quotes", quotes );

			session.persist( bookTypeName, book );
			session.persist( quoteTypeName, quote1 );
			session.persist( quoteTypeName, quote2 );

			backendMock.expectWorks( INDEX1_NAME )
					.add( "1", b -> b
							.field( "title", book.get( "title" ) )
							.objectField( "quotes", b2 -> b2
									.field( "author", quote1.get( "author" ) )
									.field( "content", quote1.get( "content" ) )
							)
							.objectField( "quotes", b2 -> b2
									.field( "author", quote2.get( "author" ) )
									.field( "content", quote2.get( "content" ) )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	public static class DynamicMapIdComparator implements Comparator<Map<String, Object>> {
		private final Comparator<Map<String, Object>> delegate =
				Comparator.comparing( (Map<String, Object> map) -> (Integer) map.get( "id" ) );

		@Override
		public int compare(Map<String, Object> o1, Map<String, Object> o2) {
			return delegate.compare( o1, o2 );
		}
	}

	public static class QuoteEmbeddable {
		public String author;
		public String content;
	}

	// Solves the problem of generating the hashcode of an entity when the map contains indirect references to itself.
	// There are probably better solutions, but this will do.
	public static class SafeHashCodeDynamicEntity extends HashMap<String, Object> {
		@Override
		public boolean equals(Object o) {
			return super.equals( o );
		}

		@Override
		public int hashCode() {
			return 0;
		}
	}
}
