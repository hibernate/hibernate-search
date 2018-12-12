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
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.builtin.MapValueExtractor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerValueExtractorBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test error cases when applying container value extractors in the {@code @GenericField} annotation.
 * <p>
 * Does not test all container value extractor types, which are tested in {@link FieldContainerValueExtractorImplicitIT}
 * and {@link FieldContainerValueExtractorExplicitIT}.
 */
@TestForIssue(jiraKey = "HSEARCH-2554")
public class FieldContainerValueExtractorBaseIT {

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
			@GenericField(extractors = @ContainerValueExtractorBeanReference(type = RawContainerValueExtractor.class))
			public Integer getId() {
				return id;
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".id" )
						.failure(
								"Cannot interpret the type arguments to the ContainerValueExtractor interface in "
										+ " implementation '" + RawContainerValueExtractor.class.getName()
										+ "'. Only the following implementations of ContainerValueExtractor are valid"
						)
						.build()
				);
	}

	private static class RawContainerValueExtractor implements ContainerValueExtractor {
		@Override
		public Stream extract(Object container) {
			throw new UnsupportedOperationException( "Should not be called" );
		}
	}

	@Test
	public void error_invalidContainerValueExtractorForType() {
		@Indexed
		class IndexedEntity {
			Integer id;
			List<Integer> numbers;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@GenericField(extractors = @ContainerValueExtractorBeanReference(type = MapValueExtractor.class))
			public List<Integer> getNumbers() {
				return numbers;
			}
		}
		SubTest.expectException(
				() -> setupHelper.withBackendMock( backendMock ).setup( IndexedEntity.class )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildSingleContextFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".numbers" )
						.failure(
								"Cannot apply the requested container value extractor '" + MapValueExtractor.class.getName()
										+ "' to type '" + List.class.getName() + "<" + Integer.class.getName() + ">'"
						)
						.build()
				);
	}

}
