/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.hibernate.search.test.util.impl.ExceptionMatcherBuilder.isException;

import java.util.function.Function;

import javax.persistence.Embeddable;

import org.apache.lucene.document.Document;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.testing.TestForIssue;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for conflict detection when generating the Elasticsearch schema.
 *
 * @author Yoann Rodiere
 */
@RunWith(Parameterized.class)
public class ElasticsearchSchemaNamingErrorsIT {

	private static final String COMPOSITE_CONCRETE_CONFLICT_MESSAGE_ID = "HSEARCH400036";
	private static final String INDEXED_EMBEDDED_BYPASS_MESSAGE_ID = "HSEARCH400054";

	private static final String CONFLICTING_FIELD_NAME = "conflictingFieldName";

	private static final String BYPASSING_FIELD_NAME = "fieldNameThatBypassesIndexedEmbeddedPrefix";
	private static final String BYPASSING_FIELD_INDEXED_EMBEDDED_PREFIX = "indexedEmbeddedPrefix.";
	private static final String BYPASSING_FIELD_DEFAULT_FIELD_NAME = "defaultFieldName";

	@Parameters(name = "With strategy {0}")
	public static IndexSchemaManagementStrategy[] strategies() {
		return IndexSchemaManagementStrategy.values();
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private SearchIntegrator integrator;

	private SearchITHelper helper = new SearchITHelper( () -> this.integrator );

	private IndexSchemaManagementStrategy strategy;

	public ElasticsearchSchemaNamingErrorsIT(IndexSchemaManagementStrategy strategy) {
		super();
		this.strategy = strategy;
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2448")
	public void detectConflict_schemaGeneration_compositeOnConcrete() throws Exception {
		testDetectConflictDuringSchemaGeneration( CompositeOnConcreteEntity.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2448")
	public void detectConflict_schemaGeneration_concreteOnComposite() throws Exception {
		testDetectConflictDuringSchemaGeneration( ConcreteOnCompositeEntity.class );
	}

	private void testDetectConflictDuringSchemaGeneration(Class<?> entityClass) {
		Assume.assumeTrue( "The strategy " + strategy + " does not involve schema generation."
				+ " No point running this test.",
				generatesSchema( strategy ) );

		thrown.expect(
				isException( SearchException.class )
						.withMessage( COMPOSITE_CONCRETE_CONFLICT_MESSAGE_ID )
						.withMessage( CONFLICTING_FIELD_NAME + "'" )
						.withMessage( entityClass.getName() )
				.build()
		);

		elasticSearchClient.index( entityClass ).registerForCleanup();
		init( strategy, entityClass );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2448")
	public void detectConflict_indexing_compositeOnConcrete() throws Exception {
		testDetectConflictDuringIndexing( CompositeOnConcreteEntity.class, CompositeOnConcreteEntity::new );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2448")
	public void detectConflict_indexing_concreteOnComposite() throws Exception {
		testDetectConflictDuringIndexing( ConcreteOnCompositeEntity.class, ConcreteOnCompositeEntity::new );
	}

	private <T> void testDetectConflictDuringIndexing(Class<T> entityClass, Function<Long, T> constructor) throws Exception {
		Assume.assumeFalse( "The strategy " + strategy + " involves schema generation,"
				+ " which means conflicts prevent search factory initialization"
				+ " and thus prevent indexing. No point running this test.",
				generatesSchema( strategy ) );

		elasticSearchClient.index( entityClass ).deleteAndCreate();
		init( strategy, entityClass );

		thrown.expect(
				isException( SearchException.class )
						.withMessage( COMPOSITE_CONCRETE_CONFLICT_MESSAGE_ID )
						.withMessage( CONFLICTING_FIELD_NAME + "'" )
						.withMessage( entityClass.getName() )
				.build()
		);

		Object newEntity = constructor.apply( 0L );
		helper.add( newEntity );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2488")
	public void detectIndexedEmbeddedPrefixBypass_schemaGeneration() {
		Assume.assumeTrue( "The strategy " + strategy + " does not involve schema generation."
				+ " No point running this test.",
				generatesSchema( strategy ) );

		Class<?> entityClass = BypassingIndexedEmbeddedPrefixEntity.class;

		thrown.expect(
				isException( SearchException.class )
						.withMessage( INDEXED_EMBEDDED_BYPASS_MESSAGE_ID )
						.withMessage( "'" + BYPASSING_FIELD_NAME + "'" )
						.withMessage( "'" + BYPASSING_FIELD_INDEXED_EMBEDDED_PREFIX + "'" )
						.withMessage( entityClass.getName() )
				.build()
		);

		elasticSearchClient.index( entityClass ).registerForCleanup();
		init( strategy, entityClass );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2488")
	public void detectIndexedEmbeddedPrefixBypass_indexing() throws Exception {
		Assume.assumeFalse( "The strategy " + strategy + " involves schema generation,"
				+ " which means @IndexedEmbedded.prefix bypasses prevent search factory initialization"
				+ " and thus prevent indexing. No point running this test.",
				generatesSchema( strategy ) );

		Class<?> entityClass = BypassingIndexedEmbeddedPrefixEntity.class;

		elasticSearchClient.index( entityClass ).deleteAndCreate();
		init( strategy, entityClass );

		thrown.expect(
				isException( SearchException.class )
						.withMessage( INDEXED_EMBEDDED_BYPASS_MESSAGE_ID )
						.withMessage( "'" + BYPASSING_FIELD_NAME + "'" )
						.withMessage( "'" + BYPASSING_FIELD_INDEXED_EMBEDDED_PREFIX + "'" )
						.withMessage( entityClass.getName() )
				.build()
		);

		Object newEntity = new BypassingIndexedEmbeddedPrefixEntity( 0 );
		helper.add( newEntity );
	}

	private boolean generatesSchema(IndexSchemaManagementStrategy strategy) {
		return !IndexSchemaManagementStrategy.NONE.equals( strategy );
	}

	private void init(IndexSchemaManagementStrategy strategy, Class<?> ... entityClasses) {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
				.addClasses( entityClasses )
				.addProperty(
						"hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
						strategy.getExternalName()
				);

		this.integrator = integratorResource.create( cfg );
	}

	@Embeddable
	private static class EmbeddedTypeWithNonConflictingField {
		@Field
		int nonConflictingField = 0;
	}

	@Embeddable
	private static class EmbeddedTypeWithConflictingField {
		@Field(name = CONFLICTING_FIELD_NAME)
		int conflictingField = 0;
	}

	/**
	 * A class for which Hibernate Search will first handle the mapping for the "concrete"
	 * (non-composite) field, and then the mapping for the composite field.
	 */
	@Indexed
	private static class CompositeOnConcreteEntity {
		@DocumentId
		Long id;

		@IndexedEmbedded(prefix = CONFLICTING_FIELD_NAME + ".")
		@Field(name = CONFLICTING_FIELD_NAME, bridge = @FieldBridge(impl = SimpleToStringBridge.class))
		EmbeddedTypeWithNonConflictingField embedded =
				new EmbeddedTypeWithNonConflictingField();

		public CompositeOnConcreteEntity(Long id) {
			this.id = id;
		}

		public static class SimpleToStringBridge implements MetadataProvidingFieldBridge {

			@Override
			public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
				luceneOptions.addFieldToDocument( name, String.valueOf( value ), document );
			}

			@Override
			public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
				builder.field( name, FieldType.STRING );
			}
		}
	}

	/**
	 * A class for which Hibernate Search will first handle the mapping for the composite
	 * field, and then the mapping for the "concrete" (non-composite) field.
	 */
	@Indexed
	private static class ConcreteOnCompositeEntity {
		@DocumentId
		Long id;

		EmbeddedTypeWithNonConflictingField embedded =
				new EmbeddedTypeWithNonConflictingField();

		public ConcreteOnCompositeEntity(Long id) {
			this.id = id;
		}

		// Hack: methods are handled first, so take advantage of that.
		@IndexedEmbedded(prefix = CONFLICTING_FIELD_NAME + ".")
		public EmbeddedTypeWithNonConflictingField getEmbedded() {
			return embedded;
		}

		@IndexedEmbedded(prefix = "")
		EmbeddedTypeWithConflictingField otherEmbedded =
				new EmbeddedTypeWithConflictingField();
	}

	@Indexed
	private static class BypassingIndexedEmbeddedPrefixEntity {
		@DocumentId
		private Integer id;

		@IndexedEmbedded(prefix = BYPASSING_FIELD_INDEXED_EMBEDDED_PREFIX)
		private BypassingIndexedEmbeddedPrefixEmbedded embedded =
				new BypassingIndexedEmbeddedPrefixEmbedded();

		public BypassingIndexedEmbeddedPrefixEntity(Integer id) {
			super();
			this.id = id;
		}
	}

	private static class BypassingIndexedEmbeddedPrefixEmbedded {
		@Field(name = BYPASSING_FIELD_DEFAULT_FIELD_NAME, bridge = @FieldBridge(impl = BypassingIndexedEmbeddedPrefixFieldBridge.class))
		private String field = "fieldValue";
	}

	public static class BypassingIndexedEmbeddedPrefixFieldBridge implements MetadataProvidingFieldBridge {
		@Override
		public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
			builder.field( BYPASSING_FIELD_NAME, FieldType.STRING );
		}

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			luceneOptions.addFieldToDocument( BYPASSING_FIELD_NAME, (String) value, document );
		}
	}

}
