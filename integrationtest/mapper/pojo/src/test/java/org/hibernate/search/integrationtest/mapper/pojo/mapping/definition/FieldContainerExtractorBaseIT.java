/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerExtractorRef;
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

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper( MethodHandles.lookup() );

	@Test
	public void error_cannotInferClassTypePattern() {
		@Indexed
		class IndexedEntity {
			Integer id;
			@DocumentId
			@GenericField(extraction = @ContainerExtraction(@ContainerExtractorRef(type = RawContainerExtractor.class)))
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
								"Cannot interpret the type arguments to the ContainerExtractor interface in "
										+ " implementation '" + RawContainerExtractor.class.getName()
										+ "'. Only the following implementations of ContainerExtractor are valid"
						)
						.build()
				);
	}

	@SuppressWarnings("rawtypes")
	private static class RawContainerExtractor implements ContainerExtractor {
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
			@GenericField(extraction = @ContainerExtraction(@ContainerExtractorRef(BuiltinContainerExtractor.MAP_VALUE)))
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
								"Cannot apply the requested container value extractor '" + BuiltinContainerExtractor.MAP_VALUE.getType().getName()
										+ "' to type '" + List.class.getName() + "<" + Integer.class.getName() + ">'"
						)
						.build()
				);
	}

	@Test
	public void invalidContainerExtractorReferencingBothBuiltinExtractorAndExplicitType() {
		@Indexed
		class IndexedEntity {
			Integer id;
			List<Integer> numbers;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField(extraction = @ContainerExtraction(
					@ContainerExtractorRef(value = BuiltinContainerExtractor.MAP_VALUE, type = RawContainerExtractor.class)
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
						.failure( "Annotation @ContainerExtractorRef references both built-in extractor (using 'MAP_VALUE') and an explicit type (using '" +
								RawContainerExtractor.class.getName() + "'). Only one of those can be defined, not both." )
						.build()
				);
	}

	@Test
	public void emptyContainerExtractorReference() {
		@Indexed
		class IndexedEntity {
			Integer id;
			List<Integer> numbers;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField(extraction = @ContainerExtraction(
					@ContainerExtractorRef()
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
								"Annotation @ContainerExtractorRef is empty",
								"The annotation must define either the built-in extractor (using 'value')",
								"or an explicit type (using 'type')"
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
					value = @ContainerExtractorRef(value = BuiltinContainerExtractor.MAP_VALUE)
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
