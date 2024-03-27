/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.MapValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test applying container value extractors in the {@code @GenericField} annotation.
 * <p>
 * Does not test all container value extractor types, which are tested in {@link FieldContainerExtractorImplicitIT}
 * and {@link FieldContainerExtractorExplicitIT}.
 */
@TestForIssue(jiraKey = "HSEARCH-2554")
class FieldContainerExtractorBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void custom() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(extraction = @ContainerExtraction(MyContainerExtractor.NAME))
			MyContainer<String> text;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "text", String.class, f -> f.multiValued( true ) )
		);

		SearchMapping mapping = setupHelper.start()
				.withConfiguration( builder -> {
					builder.containerExtractors().define( MyContainerExtractor.NAME, MyContainerExtractor.class );
				} )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			entity.text = new MyContainer<>( "value1", "value2" );
			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					// Stub backend is not supposed to use 'indexNullAs' option
					.add( "1", b -> b.field( "text", "value1", "value2" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	private static class MyContainer<T> {
		private final List<T> elements;

		@SafeVarargs
		private MyContainer(T... elements) {
			this.elements = Arrays.asList( elements );
		}

		private List<T> toList() {
			return elements;
		}
	}

	public static class MyContainerExtractor<T> implements ContainerExtractor<MyContainer<T>, T> {
		public static final String NAME = "my-container-extractor";

		@Override
		public <T1, C2> void extract(MyContainer<T> container, ValueProcessor<T1, ? super T, C2> perValueProcessor,
				T1 target, C2 context, ContainerExtractionContext extractionContext) {
			if ( container == null ) {
				return;
			}
			for ( T element : container.toList() ) {
				perValueProcessor.process( target, element, context, extractionContext );
			}
		}
	}

	@Test
	void custom_error_undefined() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			@GenericField(extraction = @ContainerExtraction("some-undefined-name"))
			Integer id;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure(
								"No container extractor with name 'some-undefined-name'."
										+ " Check that this name matches a container extractor,"
										+ " either a builtin one whose name is a constant in '"
										+ BuiltinContainerExtractors.class.getName() + "'"
										+ " or a custom one that was properly registered."
						) );
	}

	@Test
	void custom_error_cannotInferClassTypePattern() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			@GenericField(extraction = @ContainerExtraction(RawContainerExtractor.NAME))
			Integer id;
		}
		assertThatThrownBy(
				() -> setupHelper.start()
						.withConfiguration( builder -> {
							builder.containerExtractors().define( RawContainerExtractor.NAME, RawContainerExtractor.class );
						} )
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure(
								"Unable to interpret the type arguments to the ContainerExtractor interface in "
										+ " implementation '" + RawContainerExtractor.class.getName()
										+ "'. Only the following implementations of ContainerExtractor are valid"
						) );
	}

	@SuppressWarnings("rawtypes")
	private static class RawContainerExtractor implements ContainerExtractor {
		public static final String NAME = "raw-container-extractor";

		@Override
		public void extract(Object container, ValueProcessor perValueProcessor, Object target, Object context,
				ContainerExtractionContext extractionContext) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}

	@Test
	void error_invalidContainerExtractorForType() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_VALUE))
			List<Integer> numbers;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".numbers" )
						.failure( "Invalid container extractor for type '"
								+ List.class.getName() + "<" + Integer.class.getName() + ">': '"
								+ BuiltinContainerExtractors.MAP_VALUE
								+ "' (implementation class: '" + MapValueExtractor.class.getName() + "')" ) );
	}

	@Test
	void invalidContainerExtractorWithExtractNo() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(extraction = @ContainerExtraction(
					extract = ContainerExtract.NO,
					value = BuiltinContainerExtractors.MAP_VALUE
			))
			List<Integer> numbers;
		}
		assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".numbers" )
						.annotationContextAnyParameters( GenericField.class )
						.failure(
								"Unexpected extractor references",
								"extractors cannot be defined explicitly when extract = ContainerExtract.NO.",
								"Either leave 'extract' to its default value to define extractors explicitly",
								"or leave the 'extractor' list to its default, empty value to disable extraction"
						)
				);
	}

	public static class MyObjectBridge implements ValueBridge<Object, String> {
		@Override
		public String toIndexedValue(Object value, ValueBridgeToIndexedValueContext context) {
			return value.toString();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4988")
	void cycle_noExtraction() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(valueBridge = @ValueBridgeRef(type = MyObjectBridge.class),
					extraction = @ContainerExtraction(extract = ContainerExtract.NO))
			Path path;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "path", String.class )
		);

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			entity.path = Paths.get( "foo" );
			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "path", "foo" ) );
		}
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4988")
	void cycle_extraction_path() {
		@Indexed
		class IndexedEntity {
			@DocumentId
			Integer id;
			@GenericField(valueBridge = @ValueBridgeRef(type = MyObjectBridge.class))
			Path path;
		}
		assertThatThrownBy(
				() -> setupHelper.start()
						.expectCustomBeans()
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".path" )
						.failure(
								"Cyclic recursion when applying the default container extractors to type '"
										+ Path.class.getName() + "'",
								"Container extractors applied to that type and resulting in the same type: [iterable]",
								"To break the cycle, you should consider configuring container extraction explicitly"
						)
				);

	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4988")
	void cycle_extraction_generics() {
		@Indexed
		class IndexedEntity<T extends Collection<T>> {
			@DocumentId
			Integer id;
			@GenericField(valueBridge = @ValueBridgeRef(type = MyObjectBridge.class))
			T genericField;
		}
		assertThatThrownBy(
				() -> setupHelper.start()
						.expectCustomBeans()
						.setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".genericField" )
						.failure(
								"Cyclic recursion when applying the default container extractors to type '"
										+ "T (java.util.Collection<T>)'",
								"Container extractors applied to that type and resulting in the same type: [collection]",
								"To break the cycle, you should consider configuring container extraction explicitly"
						)
				);

	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4988")
	void cycle_noFalsePositive_objectArray() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity<T> {
			@DocumentId
			Integer id;
			@GenericField(valueBridge = @ValueBridgeRef(type = MyObjectBridge.class))
			T[][][] objectArray;
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "objectArray", String.class, b2 -> b2.multiValued( true ) )
		);

		SearchMapping mapping = setupHelper.start()
				.expectCustomBeans()
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity<Object> entity = new IndexedEntity<>();
			entity.id = 1;
			entity.objectArray = new Object[][][] { { { "foo" } } };
			session.indexingPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "objectArray", "foo" ) );
		}
		backendMock.verifyExpectationsMet();
	}
}
