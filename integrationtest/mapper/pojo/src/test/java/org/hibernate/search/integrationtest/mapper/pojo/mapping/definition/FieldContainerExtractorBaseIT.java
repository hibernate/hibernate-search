/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.builtin.impl.MapValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test error cases when applying container value extractors in the {@code @GenericField} annotation.
 * <p>
 * Does not test all container value extractor types, which are tested in {@link FieldContainerExtractorImplicitIT}
 * and {@link FieldContainerExtractorExplicitIT}.
 */
@TestForIssue(jiraKey = "HSEARCH-2554")
public class FieldContainerExtractorBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	@Test
	public void custom() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			Integer id;
			MyContainer<String> text;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField(extraction = @ContainerExtraction(MyContainerExtractor.NAME))
			public MyContainer<String> getText() {
				return text;
			}
		}

		backendMock.expectSchema( INDEX_NAME, b -> b
				.field( "text", String.class, f -> f.multiValued( true ) )
		);

		JavaBeanMapping mapping = setupHelper.withBackendMock( backendMock )
				.withConfiguration( builder -> {
					builder.containerExtractors().define( MyContainerExtractor.NAME, MyContainerExtractor.class );
				} )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity = new IndexedEntity();
			entity.id = 1;
			entity.text = new MyContainer<>( "value1", "value2" );
			session.getMainWorkPlan().add( entity );

			backendMock.expectWorks( INDEX_NAME )
					// Stub backend is not supposed to use 'indexNullAs' option
					.add( "1", b -> b.field( "text", "value1", "value2" ) )
					.preparedThenExecuted();
		}
		backendMock.verifyExpectationsMet();
	}

	private static class MyContainer<T> {
		private final List<T> elements;

		private MyContainer(T ... elements) {
			this.elements = Arrays.asList( elements );
		}

		private Stream<T> toStream() {
			return elements.stream();
		}
	}

	public static class MyContainerExtractor<T> implements ContainerExtractor<MyContainer<T>, T> {
		public static final String NAME = "my-container-extractor";
		@Override
		public Stream<T> extract(MyContainer<T> container) {
			return container == null ? Stream.empty() : container.toStream();
		}
	}

	@Test
	public void custom_error_undefined() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@GenericField(extraction = @ContainerExtraction("some-undefined-name"))
			public Integer getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure(
								"Cannot resolve container extractor name 'some-undefined-name'."
								+ " Check that this name matches a container extractor,"
								+ " either a builtin one whose name is a constant in '" + BuiltinContainerExtractors.class.getName() + "'"
								+ " or a custom one that was properly registered."
						)
						.build()
				);
	}

	@Test
	public void custom_error_cannotInferClassTypePattern() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@GenericField(extraction = @ContainerExtraction(RawContainerExtractor.NAME))
			public Integer getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock )
						.withConfiguration( builder -> {
							builder.containerExtractors().define( RawContainerExtractor.NAME, RawContainerExtractor.class );
						} )
						.setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure(
								"Cannot interpret the type arguments to the ContainerExtractor interface in "
										+ " implementation '" + RawContainerExtractor.class.getName()
										+ "'. Only the following implementations of ContainerExtractor are valid"
						)
						.build()
				);
	}

	@SuppressWarnings("rawtypes")
	private static class RawContainerExtractor implements ContainerExtractor {
		public static final String NAME = "raw-container-extractor";
		@Override
		public Stream extract(Object container) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}

	@Test
	public void error_invalidContainerExtractorForType() {
		@Indexed
		class IndexedEntity {
			Integer id;
			List<Integer> numbers;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField(extraction = @ContainerExtraction(BuiltinContainerExtractors.MAP_VALUE))
			public List<Integer> getNumbers() {
				return numbers;
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".numbers" )
						.failure(
								"Cannot apply the requested container value extractor '" + BuiltinContainerExtractors.MAP_VALUE
								+ "' (implementation class: '" + MapValueExtractor.class.getName()
								+ "') to type '" + List.class.getName() + "<" + Integer.class.getName() + ">'"
						)
						.build()
				);
	}

	@Test
	public void invalidContainerExtractorWithExtractNo() {
		@Indexed
		class IndexedEntity {
			Integer id;
			List<Integer> numbers;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField(extraction = @ContainerExtraction(
					extract = ContainerExtract.NO,
					value = BuiltinContainerExtractors.MAP_VALUE
			))
			public List<Integer> getNumbers() {
				return numbers;
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".numbers" )
						.annotationContextAnyParameters( GenericField.class )
						.failure(
								"Extractors cannot be defined explicitly when extract = ContainerExtract.NO.",
								"Either leave 'extract' to its default value to define extractors explicitly",
								"or leave the 'extractor' list to its default, empty value to disable extraction"
						)
						.build()
				);
	}
}
