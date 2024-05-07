/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.reference;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.reference.NestedObjectFieldReference;
import org.hibernate.search.engine.search.reference.ValueFieldReference;
import org.hibernate.search.engine.search.reference.impl.ObjectFieldReferenceImpl;
import org.hibernate.search.engine.search.reference.traits.predicate.KnnPredicateFieldReference;
import org.hibernate.search.engine.search.reference.traits.predicate.MatchPredicateFieldReference;
import org.hibernate.search.engine.search.reference.traits.projection.FieldProjectionFieldReference;
import org.hibernate.search.integrationtest.backend.tck.search.predicate.AbstractPredicateDataSet;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class FieldReferenceIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	private static final DataSet dataSet = new DataSet();

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer bulkIndexer = index.bulkIndexer();
		dataSet.contribute( index, bulkIndexer );
		bulkIndexer.join();
	}

	@Test
	void test() {
		assertThatQuery( index.query()
				.where( f -> f.bool()
						.should( f.match().field( IndexedEntity_.localDate ).matching( LocalDate.of( 2024, 1, 1 ) ) )
						.should( f.match().field( IndexedEntity_.localDate.asString() ).matching( "2024-01-02" ) )
				) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ), dataSet.docId( 1 ) );

		assertThatQuery( index.query()
				.select( f -> f.field( IndexedEntity_.string ) )
				.where( f -> f.matchAll() ) )
				.hasHitsAnyOrder( "string_0", "string_1", "string_2" );

		assertThatQuery( index.query()
				.select( f -> f.field( IndexedEntity_.stringList ).multi() )
				.where( f -> f.matchAll() ) )
				.hasHitsAnyOrder(
						List.of( "string_list_0_0", "string_list_1_0", "string_list_2_0" ),
						List.of( "string_list_0_1", "string_list_1_1", "string_list_2_1" ),
						List.of( "string_list_0_2", "string_list_1_2", "string_list_2_2" )
				);


		assertThatQuery( index.query()
				.select(
						f -> f.object( IndexedEntity_.nestedEmbeddedEntity )
								.from( f.field( IndexedEntity_.nestedEmbeddedEntity.string ),
										f.field( IndexedEntity_.nestedEmbeddedEntity.someEnum ) )
								.asList()
				)
				.where( f -> f.matchAll() ) )
				.hasHitsAnyOrder(
						List.of( "string_n_0", SomeEnum.VALUE_1 ),
						List.of( "string_n_1", SomeEnum.VALUE_2 ),
						List.of( "string_n_2", SomeEnum.VALUE_3 )
				);

		assertThatQuery( index.query()
				.select(
						f -> f.field( IndexedEntity_.vector )
				)
				.where( f -> f.knn( 10 ).field( IndexedEntity_.vector )
						.matching( new float[] { 1.0f, 1.0f, 1.0f } ) ) )
				.hasHitsAnyOrder(
						new float[] { 0.0f, 0.0f, 0.0f },
						new float[] { 10.0f, 100.0f, 1000.0f },
						new float[] { 20.0f, 200.0f, 2000.0f }
				);

		assertThatQuery( index.query()
				.where( f -> f.nested( IndexedEntity_.nestedEmbeddedEntity )
						.add( f.match().field( IndexedEntity_.nestedEmbeddedEntity.someEnum )
								.matching( SomeEnum.VALUE_1 ) ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );

		assertThatQuery( index.query()
				.select( f -> f.field( IndexedEntity_.stringProjectedAsBytes ) )
				.where( f -> f.matchAll() )
		).hasHitsAnyOrder(
				new byte[] { 97, 95, 48 },
				new byte[] { 97, 95, 50 },
				new byte[] { 97, 95, 49 }
		);

		assertThatQuery( index.query()
				.select( f -> f.field( IndexedEntity_.stringProjectedAsBytes.noConverter() ) )
				.where( f -> f.matchAll() )
		).hasHitsAnyOrder(
				"a_0", "a_1", "a_2"
		);
	}

	private static class IndexedEntity {
		private int primitiveInteger;
		private Double wrapperDouble;
		private String string;
		private String stringProjectedAsBytes;
		private LocalDate localDate;
		private float[] vector;

		private List<String> stringList;

		private EmbeddedEntity nestedEmbeddedEntity;
		private EmbeddedEntity flattenedEmbeddedEntity;

		private List<EmbeddedEntity> embeddedEntityList;

		// @IndexedEmbedded(includeDepth = 2,
		// 		structure = ObjectStructure.NESTED,
		// 		excludePaths = {
		// 				"parent.parent.primitiveInteger"
		// 		// ... (other paths so that only string and vector fields are included)
		// 		}
		// )
		private IndexedEntity parent;

	}

	private static class EmbeddedEntity {
		private String string;
		private SomeEnum someEnum;
	}

	private enum SomeEnum {
		VALUE_1, VALUE_2, VALUE_3
	}

	public static class IndexedEntity_ {
		public static ValueFieldReference1<DocumentReference, Integer, Integer, Integer> primitiveInteger;
		public static ValueFieldReference1<DocumentReference, Double, Double, Double> wrapperDouble;
		public static ValueFieldReference1<DocumentReference, String, String, String> string;
		public static ValueFieldReference1<DocumentReference, String, String, byte[]> stringProjectedAsBytes;
		public static ValueFieldReference1<DocumentReference, LocalDate, LocalDate, LocalDate> localDate;
		public static ValueFieldReference2<DocumentReference, float[], float[], float[]> vector;

		public static ValueFieldReference1<DocumentReference, String, String, String> stringList;
		public static NestedEmbeddedEntity_ nestedEmbeddedEntity;
		public static EmbeddedEntityList_ embeddedEntityList;

		public static FlattenedEmbeddedEntity_ flattenedEmbeddedEntity;

		public static Parent_ parent;

		static {
			primitiveInteger = ValueFieldReference1.of( "primitiveInteger", DocumentReference.class, Integer.class,
					Integer.class, Integer.class );
			wrapperDouble = ValueFieldReference1.of( "wrapperDouble", DocumentReference.class, Double.class, Double.class,
					Double.class );
			string = ValueFieldReference1.of( "string", DocumentReference.class, String.class, String.class, String.class );
			stringProjectedAsBytes =
					ValueFieldReference1.of( "stringProjectedAsBytes", DocumentReference.class, String.class, String.class,
							byte[].class );
			localDate = ValueFieldReference1.of( "localDate", DocumentReference.class, LocalDate.class, LocalDate.class,
					LocalDate.class );
			vector = ValueFieldReference2.of( "vector", DocumentReference.class, float[].class, float[].class, float[].class );
			stringList =
					ValueFieldReference1.of( "stringList", DocumentReference.class, String.class, String.class, String.class );

			flattenedEmbeddedEntity = new FlattenedEmbeddedEntity_( "flattenedEmbeddedEntity" );
			nestedEmbeddedEntity = new NestedEmbeddedEntity_( "nestedEmbeddedEntity" );
			embeddedEntityList = new EmbeddedEntityList_( "embeddedEntityList" );
			parent = new Parent_( "parent" );
		}

		public static class NestedEmbeddedEntity_ extends ObjectFieldReferenceImpl implements NestedObjectFieldReference {

			public ValueFieldReference1<DocumentReference, String, String, String> string;
			public ValueFieldReference1<DocumentReference, SomeEnum, String, SomeEnum> someEnum;

			private NestedEmbeddedEntity_(String absolutePath) {
				super( absolutePath );
				this.string = ValueFieldReference1.of( absolutePath + ".string", DocumentReference.class, String.class,
						String.class, String.class );
				this.someEnum =
						ValueFieldReference1.of( absolutePath + ".someEnum", DocumentReference.class, SomeEnum.class,
								String.class, SomeEnum.class );
			}
		}

		public static class EmbeddedEntityList_ extends ObjectFieldReferenceImpl implements NestedObjectFieldReference {

			public ValueFieldReference<String, String, String> string;
			public ValueFieldReference<SomeEnum, String, SomeEnum> someEnum;

			private EmbeddedEntityList_(String absolutePath) {
				super( absolutePath );
				this.string = ValueFieldReference.of( absolutePath + ".string", String.class, String.class, String.class );
				this.someEnum =
						ValueFieldReference.of( absolutePath + ".someEnum", SomeEnum.class, String.class, SomeEnum.class );
			}
		}

		public static class FlattenedEmbeddedEntity_ extends ObjectFieldReferenceImpl {

			public ValueFieldReference<String, String, String> string;
			public ValueFieldReference<SomeEnum, String, SomeEnum> someEnum;

			private FlattenedEmbeddedEntity_(String absolutePath) {
				super( absolutePath );
				this.string = ValueFieldReference.of( absolutePath + ".string", String.class, String.class, String.class );
				this.someEnum =
						ValueFieldReference.of( absolutePath + ".someEnum", SomeEnum.class, String.class, SomeEnum.class );
			}
		}

		public static class Parent_ extends ObjectFieldReferenceImpl implements NestedObjectFieldReference {
			public ValueFieldReference<Integer, Integer, Integer> primitiveInteger;
			public ValueFieldReference<Double, Double, Double> wrapperDouble;
			public ValueFieldReference<String, String, String> string;
			public ValueFieldReference<String, String, byte[]> stringProjectedAsBytes;
			public ValueFieldReference<LocalDate, LocalDate, LocalDate> localDate;
			public ValueFieldReference<float[], float[], float[]> vector;

			public ValueFieldReference<String, String, String> stringList;
			public NestedEmbeddedEntity_ nestedEmbeddedEntity;
			public EmbeddedEntityList_ embeddedEntityList;

			public FlattenedEmbeddedEntity_ flattenedEmbeddedEntity;

			// an extra _ since otherwise the cyclic nested property class will clash with the "previous one"
			public Parent__ parent;

			private Parent_(String absolutePath) {
				super( absolutePath );
				primitiveInteger = ValueFieldReference.of( absolutePath + ".primitiveInteger", Integer.class, Integer.class,
						Integer.class );
				wrapperDouble =
						ValueFieldReference.of( absolutePath + ".wrapperDouble", Double.class, Double.class, Double.class );
				string = ValueFieldReference.of( absolutePath + ".string", String.class, String.class, String.class );
				stringProjectedAsBytes =
						ValueFieldReference.of( "stringProjectedAsBytes", String.class, String.class, byte[].class );
				localDate = ValueFieldReference.of( absolutePath + ".localDate", LocalDate.class, LocalDate.class,
						LocalDate.class );
				vector = ValueFieldReference.of( absolutePath + ".vector", float[].class, float[].class, float[].class );
				stringList = ValueFieldReference.of( absolutePath + ".stringList", String.class, String.class, String.class );

				flattenedEmbeddedEntity = new FlattenedEmbeddedEntity_( absolutePath + ".flattenedEmbeddedEntity" );
				nestedEmbeddedEntity = new NestedEmbeddedEntity_( absolutePath + ".nestedEmbeddedEntity" );
				embeddedEntityList = new EmbeddedEntityList_( absolutePath + ".embeddedEntityList" );

				parent = new Parent__( absolutePath + ".parent" );
			}

			public static class NestedEmbeddedEntity_ extends ObjectFieldReferenceImpl implements NestedObjectFieldReference {

				public ValueFieldReference<String, String, String> string;
				public ValueFieldReference<SomeEnum, String, SomeEnum> someEnum;

				private NestedEmbeddedEntity_(String absolutePath) {
					super( absolutePath );
					this.string = ValueFieldReference.of( absolutePath + ".string", String.class, String.class, String.class );
					this.someEnum =
							ValueFieldReference.of( absolutePath + ".someEnum", SomeEnum.class, String.class, SomeEnum.class );
				}
			}

			public static class EmbeddedEntityList_ extends ObjectFieldReferenceImpl implements NestedObjectFieldReference {

				public ValueFieldReference<String, String, String> string;
				public ValueFieldReference<SomeEnum, String, SomeEnum> someEnum;

				private EmbeddedEntityList_(String absolutePath) {
					super( absolutePath );
					this.string = ValueFieldReference.of( absolutePath + ".string", String.class, String.class, String.class );
					this.someEnum =
							ValueFieldReference.of( absolutePath + ".someEnum", SomeEnum.class, String.class, SomeEnum.class );
				}
			}

			public static class FlattenedEmbeddedEntity_ extends ObjectFieldReferenceImpl {

				public ValueFieldReference<String, String, String> string;
				public ValueFieldReference<SomeEnum, String, SomeEnum> someEnum;

				private FlattenedEmbeddedEntity_(String absolutePath) {
					super( absolutePath );
					this.string = ValueFieldReference.of( absolutePath + ".string", String.class, String.class, String.class );
					this.someEnum =
							ValueFieldReference.of( absolutePath + ".someEnum", SomeEnum.class, String.class, SomeEnum.class );
				}
			}

			public static class Parent__ extends ObjectFieldReferenceImpl implements NestedObjectFieldReference {

				public ValueFieldReference<String, String, String> string;
				public ValueFieldReference<float[], float[], float[]> vector;

				private Parent__(String absolutePath) {
					super( absolutePath );
					this.string = ValueFieldReference.of( absolutePath + ".string", String.class, String.class, String.class );
					this.vector =
							ValueFieldReference.of( absolutePath + ".vector", float[].class, float[].class, float[].class );
				}
			}
		}
	}

	// =================================================================================================================

	private static class IndexBinding {
		private static class IndexBindingFields {
			IndexObjectFieldReference self;

			IndexFieldReference<Integer> primitiveInteger;
			IndexFieldReference<Double> wrapperDouble;
			IndexFieldReference<String> string;
			IndexFieldReference<String> stringProjectedAsBytes;
			IndexFieldReference<LocalDate> localDate;
			IndexFieldReference<String> stringList;
			IndexFieldReference<float[]> vector;

			IndexObjectFieldReference nestedEmbeddedEntity;

			IndexFieldReference<String> nestedEmbeddedEntityString;
			IndexFieldReference<String> nestedEmbeddedEntityEnum;

			IndexObjectFieldReference flattenedEmbeddedEntity;
			IndexFieldReference<String> flattenedEmbeddedEntityString;
			IndexFieldReference<String> flattenedEmbeddedEntityEnum;

			IndexObjectFieldReference nestedEmbeddedListEntity;
			IndexFieldReference<String> nestedEmbeddedListEntityString;
			IndexFieldReference<String> nestedEmbeddedListEntityEnum;

			IndexBindingFields parent;
		}

		IndexBindingFields fields;

		IndexBinding(IndexSchemaElement element) {
			fields = indexedEntity( element );
			IndexSchemaObjectField parent = element.objectField( "parent", ObjectStructure.NESTED );

			fields.parent = indexedEntity( parent );
			fields.parent.self = parent.toReference();

			parent = parent.objectField( "parent", ObjectStructure.NESTED );

			fields.parent.parent = indexedEntityWithExcludes( parent );
			fields.parent.parent.self = parent.toReference();

		}

		IndexBindingFields indexedEntity(IndexSchemaElement element) {
			IndexBindingFields fields = new IndexBindingFields();
			fields.primitiveInteger =
					element.field( "primitiveInteger", f -> f.asInteger().projectable( Projectable.YES ) ).toReference();
			fields.wrapperDouble =
					element.field( "wrapperDouble", f -> f.asDouble().projectable( Projectable.YES ) ).toReference();
			fields.string = element.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();
			fields.stringProjectedAsBytes =
					element.field( "stringProjectedAsBytes", f -> f.asString().projectable( Projectable.YES )
							.projectionConverter( byte[].class, StringConverter.INSTANCE ) ).toReference();
			fields.localDate = element.field( "localDate", f -> f.asLocalDate().projectable( Projectable.YES ) ).toReference();
			fields.stringList =
					element.field( "stringList", f -> f.asString().projectable( Projectable.YES ) ).multiValued().toReference();
			fields.vector = element
					.field( "vector",
							f -> f.asFloatVector().dimension( 3 ).vectorSimilarity( VectorSimilarity.L2 )
									.projectable( Projectable.YES ) )
					.toReference();


			// EmbeddedEntity nestedEmbeddedEntity
			IndexSchemaObjectField nestedEmbedded = element.objectField( "nestedEmbeddedEntity", ObjectStructure.NESTED );
			fields.nestedEmbeddedEntity = nestedEmbedded.toReference();
			fields.nestedEmbeddedEntityString =
					nestedEmbedded.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();

			EnumConverter converter = new EnumConverter();
			fields.nestedEmbeddedEntityEnum = nestedEmbedded.field(
					"someEnum",
					f -> f.asString().projectable( Projectable.YES ).dslConverter( SomeEnum.class, converter )
							.projectionConverter( SomeEnum.class, converter )
			).toReference();

			// EmbeddedEntity flattenedEmbeddedEntity
			IndexSchemaObjectField flattenedEmbedded =
					element.objectField( "flattenedEmbeddedEntity", ObjectStructure.FLATTENED );
			fields.flattenedEmbeddedEntity = flattenedEmbedded.toReference();
			fields.flattenedEmbeddedEntityString =
					flattenedEmbedded.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();

			fields.flattenedEmbeddedEntityEnum = flattenedEmbedded.field( "someEnum",
					f -> f.asString().projectable( Projectable.YES ).dslConverter( SomeEnum.class, converter )
							.projectionConverter( SomeEnum.class, converter ) )
					.toReference();


			// List<EmbeddedEntity> embeddedEntityList
			IndexSchemaObjectField nestedEmbeddedList =
					element.objectField( "embeddedEntityList", ObjectStructure.NESTED ).multiValued();
			fields.nestedEmbeddedListEntity = nestedEmbeddedList.toReference();
			fields.nestedEmbeddedListEntityString =
					nestedEmbeddedList.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();

			fields.nestedEmbeddedListEntityEnum = nestedEmbeddedList.field(
					"someEnum",
					f -> f.asString().projectable( Projectable.YES ).dslConverter( SomeEnum.class, converter )
							.projectionConverter( SomeEnum.class, converter ) )
					.toReference();

			return fields;
		}

		IndexBindingFields indexedEntityWithExcludes(IndexSchemaElement element) {
			IndexBindingFields fields = new IndexBindingFields();
			fields.string = element.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();
			fields.vector = element
					.field( "vector",
							f -> f.asFloatVector().dimension( 3 ).vectorSimilarity( VectorSimilarity.L2 )
									.projectable( Projectable.YES ) )
					.toReference();
			return fields;
		}
	}

	public static final class DataSet extends AbstractPredicateDataSet {

		public DataSet() {
			super( null );
		}

		public void contribute(SimpleMappedIndex<IndexBinding> index, BulkIndexer indexer) {
			indexer.add( docId( 0 ), routingKey, document -> initDocument( index, document, 0 ) );
			indexer.add( docId( 1 ), routingKey, document -> initDocument( index, document, 1 ) );
			indexer.add( docId( 2 ), routingKey, document -> initDocument( index, document, 2 ) );
		}

		private void initDocument(SimpleMappedIndex<IndexBinding> index, DocumentElement document, int docOrdinal) {
			IndexBinding binding = index.binding();

			init( document, docOrdinal, binding.fields );

			// first parent :
			DocumentElement parent = document.addObject( binding.fields.parent.self );
			init( parent, docOrdinal, binding.fields.parent );

			parent = parent.addObject( binding.fields.parent.parent.self );

			parent.addValue( binding.fields.parent.parent.string, "string_" + docOrdinal );
			parent.addValue( binding.fields.parent.parent.vector,
					new float[] { docOrdinal * 10.0f, docOrdinal * 100.0f, docOrdinal * 1000.0f } );
		}

		private static void init(DocumentElement document, int docOrdinal, IndexBinding.IndexBindingFields fields) {
			document.addValue( fields.primitiveInteger, docOrdinal );
			document.addValue( fields.wrapperDouble, docOrdinal * 100.0 );
			document.addValue( fields.string, "string_" + docOrdinal );
			document.addValue( fields.stringProjectedAsBytes, "a_" + docOrdinal );
			document.addValue( fields.localDate, LocalDate.of( 2024, 1, docOrdinal + 1 ) );
			document.addValue( fields.stringList, "string_list_0_" + docOrdinal );
			document.addValue( fields.stringList, "string_list_1_" + docOrdinal );
			document.addValue( fields.stringList, "string_list_2_" + docOrdinal );
			document.addValue( fields.vector, new float[] { docOrdinal * 10.0f, docOrdinal * 100.0f, docOrdinal * 1000.0f } );

			SomeEnum[] enums = SomeEnum.values();

			DocumentElement nestedEmbeddedEntity = document.addObject( fields.nestedEmbeddedEntity );
			nestedEmbeddedEntity.addValue( fields.nestedEmbeddedEntityString, "string_n_" + docOrdinal );
			nestedEmbeddedEntity.addValue( fields.nestedEmbeddedEntityEnum, enums[docOrdinal % enums.length].name() );

			DocumentElement flattenedEmbeddedEntity = document.addObject( fields.flattenedEmbeddedEntity );

			flattenedEmbeddedEntity.addValue( fields.flattenedEmbeddedEntityString, "string_f_" + docOrdinal );
			flattenedEmbeddedEntity.addValue( fields.flattenedEmbeddedEntityEnum, enums[docOrdinal % enums.length].name() );

			for ( int i = 0; i < 5; i++ ) {
				DocumentElement element = document.addObject( fields.nestedEmbeddedListEntity );

				element.addValue( fields.nestedEmbeddedListEntityString, "string_f_" + docOrdinal );
				element.addValue( fields.nestedEmbeddedListEntityEnum, enums[docOrdinal % enums.length].name() );
			}
		}
	}


	private static class StringConverter implements FromDocumentValueConverter<String, byte[]> {
		private static final StringConverter INSTANCE = new StringConverter();

		@Override
		public byte[] fromDocumentValue(String value, FromDocumentValueConvertContext context) {
			return value == null ? null : value.getBytes( StandardCharsets.UTF_8 );
		}
	}

	private static class EnumConverter
			implements ToDocumentValueConverter<SomeEnum, String>,
			FromDocumentValueConverter<String, SomeEnum> {

		@Override
		public String toDocumentValue(SomeEnum value, ToDocumentValueConvertContext context) {
			return value == null ? null : value.name();
		}

		@Override
		public SomeEnum fromDocumentValue(String value, FromDocumentValueConvertContext context) {
			return value == null ? null : SomeEnum.valueOf( value );
		}
	}

	public static class ValueFieldReference1<E, T, V, P> extends TypedFieldReference1<E, T, P> {

		public static <E, T, V, P> ValueFieldReference1<E, T, V, P> of(
				String path,
				Class<E> documentReferenceClass,
				Class<T> t,
				Class<V> v,
				Class<P> p) {
			return new ValueFieldReference1<>( path, documentReferenceClass, t, v, p );
		}

		private final TypedFieldReference1<E, V, V> noConverter;
		private final TypedFieldReference1<E, String, String> string;

		public ValueFieldReference1(String absolutePath, Class<E> containing, Class<T> inputType, Class<V> indexType,
				Class<P> projectionType) {
			super( absolutePath, ValueConvert.YES, containing, inputType, projectionType );
			this.noConverter = new TypedFieldReference1<>( absolutePath, ValueConvert.NO, containing, indexType, indexType );
			this.string =
					new TypedFieldReference1<>( absolutePath, ValueConvert.PARSE, containing, String.class, String.class );
		}

		public TypedFieldReference1<E, V, V> noConverter() {
			return noConverter;
		}


		public TypedFieldReference1<E, String, String> asString() {
			return string;
		}

	}

	public static class TypedFieldReference1<E, T, P>
			implements FieldProjectionFieldReference<E, P>,
			MatchPredicateFieldReference<E, T> {

		private final String absolutePath;
		private final ValueConvert valueConvert;
		private final Class<E> containing;
		private final Class<T> input;
		private final Class<P> projection;

		public TypedFieldReference1(String absolutePath, ValueConvert valueConvert, Class<E> containing, Class<T> input,
				Class<P> projection) {
			this.absolutePath = absolutePath;
			this.valueConvert = valueConvert;
			this.containing = containing;
			this.input = input;
			this.projection = projection;
		}

		@Override
		public String absolutePath() {
			return absolutePath;
		}

		@Override
		public Class<T> predicateType() {
			return input;
		}

		@Override
		public ValueConvert valueConvert() {
			return valueConvert;
		}

		@Override
		public Class<P> projectionType() {
			return projection;
		}

		@Override
		public Class<E> containing() {
			return containing;
		}
	}



	public static class ValueFieldReference2<E, T, V, P> extends TypedFieldReference2<E, T, P> {

		public static <E, T, V, P> ValueFieldReference2<E, T, V, P> of(
				String path,
				Class<E> documentReferenceClass,
				Class<T> t,
				Class<V> v,
				Class<P> p) {
			return new ValueFieldReference2<>( path, documentReferenceClass, t, v, p );
		}

		private final TypedFieldReference2<E, V, V> noConverter;
		private final TypedFieldReference2<E, String, String> string;

		public ValueFieldReference2(String absolutePath, Class<E> containing, Class<T> inputType, Class<V> indexType,
				Class<P> projectionType) {
			super( absolutePath, ValueConvert.YES, containing, inputType, projectionType );
			this.noConverter = new TypedFieldReference2<>( absolutePath, ValueConvert.NO, containing, indexType, indexType );
			this.string =
					new TypedFieldReference2<>( absolutePath, ValueConvert.PARSE, containing, String.class, String.class );
		}

		public TypedFieldReference2<E, V, V> noConverter() {
			return noConverter;
		}


		public TypedFieldReference2<E, String, String> asString() {
			return string;
		}

	}

	public static class TypedFieldReference2<E, T, P>
			implements FieldProjectionFieldReference<E, P>,
			KnnPredicateFieldReference<E, T> {

		private final String absolutePath;
		private final ValueConvert valueConvert;
		private final Class<E> containing;
		private final Class<T> input;
		private final Class<P> projection;

		public TypedFieldReference2(String absolutePath, ValueConvert valueConvert, Class<E> containing, Class<T> input,
				Class<P> projection) {
			this.absolutePath = absolutePath;
			this.valueConvert = valueConvert;
			this.containing = containing;
			this.input = input;
			this.projection = projection;
		}

		@Override
		public String absolutePath() {
			return absolutePath;
		}

		@Override
		public Class<T> predicateType() {
			return input;
		}

		@Override
		public ValueConvert valueConvert() {
			return valueConvert;
		}

		@Override
		public Class<P> projectionType() {
			return projection;
		}

		@Override
		public Class<E> containing() {
			return containing;
		}
	}

}
