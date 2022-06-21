/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
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
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
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
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void custom() {
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
		private MyContainer(T ... elements) {
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
	public void custom_error_undefined() {
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
								+ " either a builtin one whose name is a constant in '" + BuiltinContainerExtractors.class.getName() + "'"
								+ " or a custom one that was properly registered."
						) );
	}

	@Test
	public void custom_error_cannotInferClassTypePattern() {
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
	public void error_invalidContainerExtractorForType() {
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
	public void invalidContainerExtractorWithExtractNo() {
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
}
