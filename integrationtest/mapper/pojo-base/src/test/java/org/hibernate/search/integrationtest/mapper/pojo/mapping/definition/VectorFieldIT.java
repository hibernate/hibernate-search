/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class VectorFieldIT {

	private static final String INDEX_NAME = "IndexName";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void defaultAttributes() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 5)
			byte[] vector;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "vector", byte[].class, f -> f.dimension( 5 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void efConstruction() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 5, efConstruction = 10)
			byte[] vector;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "vector", byte[].class, f -> f.dimension( 5 ).efConstruction( 10 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void m() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 5, m = 10)
			byte[] vector;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "vector", byte[].class, f -> f.dimension( 5 ).m( 10 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void name() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 5, name = "explicitName")
			byte[] vector;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "explicitName", byte[].class, f -> f.dimension( 5 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void projectable() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 4, projectable = Projectable.YES)
			byte[] projectableYes;
			@VectorField(dimension = 4, projectable = Projectable.NO)
			byte[] projectableNo;
			@VectorField(dimension = 4, projectable = Projectable.DEFAULT)
			byte[] projectableDefault;
			@VectorField(dimension = 4)
			byte[] implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "projectableYes", byte[].class, f -> f.dimension( 4 ).projectable( Projectable.YES ) )
				.field( "projectableNo", byte[].class, f -> f.dimension( 4 ).projectable( Projectable.NO ) )
				.field( "projectableDefault", byte[].class, f -> f.dimension( 4 ) )
				.field( "implicit", byte[].class, f -> f.dimension( 4 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void searchable() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 4, searchable = Searchable.YES)
			byte[] searchableYes;
			@VectorField(dimension = 4, searchable = Searchable.NO)
			byte[] searchableNo;
			@VectorField(dimension = 4, searchable = Searchable.DEFAULT)
			byte[] searchableDefault;
			@VectorField(dimension = 4)
			byte[] implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "searchableYes", byte[].class, f -> f.dimension( 4 ).searchable( Searchable.YES ) )
				.field( "searchableNo", byte[].class, f -> f.dimension( 4 ).searchable( Searchable.NO ) )
				.field( "searchableDefault", byte[].class, f -> f.dimension( 4 ) )
				.field( "implicit", byte[].class, f -> f.dimension( 4 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void vectorSimilarity() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 4, vectorSimilarity = VectorSimilarity.L2)
			float[] vectorSimilarityL2;
			@VectorField(dimension = 4, vectorSimilarity = VectorSimilarity.COSINE)
			float[] vectorSimilarityCosine;
			@VectorField(dimension = 4, vectorSimilarity = VectorSimilarity.DOT_PRODUCT)
			float[] vectorSimilarityInnerProduct;
			@VectorField(dimension = 4, vectorSimilarity = VectorSimilarity.DEFAULT)
			float[] vectorSimilarityDefault;
			@VectorField(dimension = 4)
			float[] implicit;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "vectorSimilarityL2", float[].class, f -> f.dimension( 4 ).vectorSimilarity( VectorSimilarity.L2 ) )
				.field( "vectorSimilarityCosine", float[].class,
						f -> f.dimension( 4 ).vectorSimilarity( VectorSimilarity.COSINE ) )
				.field( "vectorSimilarityInnerProduct", float[].class,
						f -> f.dimension( 4 ).vectorSimilarity( VectorSimilarity.DOT_PRODUCT ) )
				.field( "vectorSimilarityDefault", float[].class, f -> f.dimension( 4 ) )
				.field( "implicit", float[].class, f -> f.dimension( 4 ) )
		);
		setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void customBridge_explicitFieldType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(valueBinder = @ValueBinderRef(type = ValidTypeBridge.ExplicitFieldTypeBinder.class))
			List<Byte> bytes;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "bytes", byte[].class, f -> f.dimension( 2 ) )
		);
		setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void customBridge_withParams_annotationMapping() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(valueBinder = @ValueBinderRef(type = ParametricBridge.ParametricBinder.class,
					params = @Param(name = "dimension", value = "2")))
			List<Byte> bytes;

			@VectorField(valueBinder = @ValueBinderRef(type = ParametricBridge.ParametricBinder.class,
					params = @Param(name = "dimension", value = "5")))
			List<Float> floats;

			IndexedEntity() {
			}

			IndexedEntity(Integer id, List<Byte> bytes, List<Float> floats) {
				this.id = id;
				this.bytes = bytes;
				this.floats = floats;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "floats", float[].class, f -> f.dimension( 5 ) )
				.field( "bytes", float[].class, f -> f.dimension( 2 ) ) );
		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity(
					1, Arrays.asList( (byte) 1, (byte) 2 ), Arrays.asList( 1.0f, 1.0f, 1.0f, 1.0f, 1.0f )
			);
			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "bytes", new float[] { 1, 2 } )
							.field( "floats", new float[] { 1.0f, 1.0f, 1.0f, 1.0f, 1.0f } ) );

		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	void customBridge_implicitFieldType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 2,
					valueBinder = @ValueBinderRef(type = ValidImplicitTypeBridge.ValidImplicitTypeBinder.class))
			Collection<Float> floats;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "floats", float[].class, f -> f.dimension( 2 ) )
		);
		setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void customBridge_withParams_programmaticMapping() {
		class IndexedEntity {
			Integer id;
			Collection<Float> floats;

			IndexedEntity() {
			}

			IndexedEntity(Integer id, Collection<Float> floats) {
				this.id = id;
				this.floats = floats;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "floats", float[].class, f -> f.dimension( 2 ) ) );
		SearchMapping mapping = setupHelper.start()
				.withConfiguration( builder -> {
					TypeMappingStep indexedEntity = builder.programmaticMapping().type( IndexedEntity.class );
					indexedEntity.searchEntity();
					indexedEntity.indexed().index( INDEX_NAME );
					indexedEntity.property( "id" ).documentId();
					indexedEntity.property( "floats" ).vectorField( 2 ).valueBinder(
							new ValidImplicitTypeBridge.ValidImplicitTypeBinder(),
							Collections.emptyMap()
					);
				} )
				.expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity( 1, Arrays.asList( 1.0f, 2.0f ) );
			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "floats", new float[] { 1.0f, 2.0f } ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	void defaultBridge_invalidFieldType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 5)
			Integer notVector;
		}

		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".notVector" )
						// NOTE: this is an exception from the IndexFieldTypeFactory implementation, hence it is backend-specific and in this case
						// it is from the stub-backend.
						.failure( "No built-in vector index field type for class: 'java.lang.Integer'." ) );
	}

	@Test
	void customBridge_implicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 5, valueBridge = @ValueBridgeRef(type = InvalidTypeBridge.class))
			List<Byte> bytes;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".bytes" )
						// NOTE: this is an exception from the IndexFieldTypeFactory implementation, hence it is backend-specific and in this case
						// it is from the stub-backend.
						.failure( "No built-in vector index field type for class: 'java.lang.Integer'." ) );
	}

	@Test
	void customBridge_explicitFieldType_invalid() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(valueBinder = @ValueBinderRef(type = InvalidTypeBridge.ExplicitFieldTypeBinder.class))
			List<Byte> bytes;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".bytes" )
						.failure(
								"Unable to apply property mapping: this property mapping must target an index field of vector type, but the resolved field type is non-vector",
								"This generally means you need to use a different field annotation"
										+ " or to convert property values using a custom ValueBridge or ValueBinder",
								"If you are already using a custom ValueBridge or ValueBinder, check its field type",
								"encountered type DSL step '",
								"expected interface '" + VectorFieldTypeOptionsStep.class.getName() + "'"
						) );
	}

	@Test
	void customBridge_implicitFieldType_generic() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(valueBridge = @ValueBridgeRef(type = GenericTypeBridge.class))
			String property;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".property" )
						.failure( "Unable to infer index field type for value bridge '"
								+ GenericTypeBridge.TOSTRING + "':"
								+ " this bridge implements ValueBridge<V, F>,"
								+ " but sets the generic type parameter F to 'T'."
								+ " The index field type can only be inferred automatically"
								+ " when this type parameter is set to a raw class."
								+ " Use a ValueBinder to set the index field type explicitly,"
								+ " or set the type parameter F to a definite, raw type." ) );
	}

	@Test
	void valueExtractorsEnabled() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 2, extraction = @ContainerExtraction(extract = ContainerExtract.DEFAULT,
					value = { BuiltinContainerExtractors.OPTIONAL }))
			Optional<byte[]> bytes;


			@VectorField(dimension = 5, extraction = @ContainerExtraction(extract = ContainerExtract.DEFAULT,
					value = { BuiltinContainerExtractors.OPTIONAL }))
			Optional<float[]> floats;

			IndexedEntity() {
			}

			IndexedEntity(Integer id, byte[] bytes, float[] floats) {
				this.id = id;
				this.bytes = Optional.ofNullable( bytes );
				this.floats = Optional.ofNullable( floats );
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b.field( "floats", float[].class, f -> f.dimension( 5 ) )
				.field( "bytes", byte[].class, f -> f.dimension( 2 ) ) );
		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity(
					1, new byte[] { (byte) 1, (byte) 2 }, null
			);
			IndexedEntity entity2 = new IndexedEntity(
					2, null, new float[] { 1.0f, 1.0f, 1.0f, 1.0f, 1.0f }
			);
			session.indexingPlan().add( entity1 );
			session.indexingPlan().add( entity2 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "bytes", new byte[] { 1, 2 } ) )
					.add( "2", b -> b.field( "floats", new float[] { 1.0f, 1.0f, 1.0f, 1.0f, 1.0f } ) );

		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	void valueExtractorsEnabled_defaultExtractorPathNotSupported() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 5, extraction = @ContainerExtraction(extract = ContainerExtract.DEFAULT))
			Optional<float[]> floats;
		}

		assertThatThrownBy( () -> setupHelper.start().expectCustomBeans().setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".floats" )
						.annotationContextAnyParameters( VectorField.class )
						.failure( "Vector fields require an explicit extraction path being specified, "
								+ "i.e. extraction must be set to DEFAULT and a nonempty array of container value extractor names provided, "
								+ "e.g. @ContainerExtraction(extract = ContainerExtract.DEFAULT, value = { ... })." ) );
	}

	@Test
	void customBridge_dimensionFromAnnotationTypeInBridge() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@VectorField(dimension = 3,
					valueBinder = @ValueBinderRef(type = ListTypeBridgeDimensionFromAnnotation.ExplicitFieldTypeBinder.class))
			List<Float> floats;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "floats", float[].class, f -> f.dimension( 3 ) )
		);
		setupHelper.start().expectCustomBeans().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@SuppressWarnings("rawtypes")
	public static class ValidTypeBridge implements ValueBridge<List, byte[]> {
		@Override
		public byte[] toIndexedValue(List value, ValueBridgeToIndexedValueContext context) {
			if ( value == null ) {
				return null;
			}
			byte[] bytes = new byte[value.size()];
			int index = 0;
			for ( Object o : value ) {
				bytes[index++] = Byte.parseByte( Objects.toString( o, null ) );
			}
			return bytes;
		}

		public static class ExplicitFieldTypeBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( List.class, new ValidTypeBridge(), context.typeFactory().asByteVector().dimension( 2 ) );
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static class ListTypeBridgeDimensionFromAnnotation implements ValueBridge<List, float[]> {
		@Override
		public float[] toIndexedValue(List value, ValueBridgeToIndexedValueContext context) {
			if ( value == null ) {
				return null;
			}
			float[] floats = new float[value.size()];
			int index = 0;
			for ( Object o : value ) {
				floats[index++] = Byte.parseByte( Objects.toString( o, null ) );
			}
			return floats;
		}

		public static class ExplicitFieldTypeBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( List.class, new ListTypeBridgeDimensionFromAnnotation(),
						context.typeFactory().asFloatVector() );
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static class ParametricBridge implements ValueBridge<List, float[]> {

		@Override
		public float[] toIndexedValue(List value, ValueBridgeToIndexedValueContext context) {
			if ( value == null ) {
				return null;
			}
			float[] floats = new float[value.size()];
			int index = 0;
			for ( Object o : value ) {
				floats[index++] = Float.parseFloat( Objects.toString( o, null ) );
			}
			return floats;
		}

		public static class ParametricBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( List.class, new ParametricBridge(),
						context.typeFactory().asFloatVector().dimension( extractDimension( context ) )
				);
			}
		}

		private static int extractDimension(ValueBindingContext<?> context) {
			return Integer.parseInt( context.param( "dimension", String.class ) );
		}
	}

	@SuppressWarnings("rawtypes")
	public static class ValidImplicitTypeBridge implements ValueBridge<Collection, float[]> {

		public static class ValidImplicitTypeBinder implements ValueBinder {

			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( Collection.class, new ValidImplicitTypeBridge() );
			}
		}

		@Override
		public float[] toIndexedValue(Collection value, ValueBridgeToIndexedValueContext context) {
			if ( value == null ) {
				return null;
			}
			float[] result = new float[value.size()];
			int index = 0;
			for ( Object o : value ) {
				result[index++] = Float.parseFloat( Objects.toString( o, null ) );
			}
			return result;
		}

		@Override
		public Collection fromIndexedValue(float[] value, ValueBridgeFromIndexedValueContext context) {
			if ( value == null ) {
				return null;
			}
			List<Float> floats = new ArrayList<>( value.length );
			for ( float v : value ) {
				floats.add( v );
			}
			return floats;
		}
	}

	@SuppressWarnings("rawtypes")
	public static class InvalidTypeBridge implements ValueBridge<List, Integer> {
		@Override
		public Integer toIndexedValue(List value, ValueBridgeToIndexedValueContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}

		public static class ExplicitFieldTypeBinder implements ValueBinder {
			@Override
			public void bind(ValueBindingContext<?> context) {
				context.bridge( List.class, new InvalidTypeBridge(), context.typeFactory().asInteger() );
			}
		}
	}

	public static class GenericTypeBridge<T> implements ValueBridge<String, T> {
		private static final String TOSTRING = "<GenericTypeBridge toString() result>";

		@Override
		public String toString() {
			return TOSTRING;
		}

		@Override
		public T toIndexedValue(String value, ValueBridgeToIndexedValueContext context) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}

}
