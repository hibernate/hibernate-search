/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentReferenceProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-4574")
public class ProjectionConstructorDocumentReferenceProjectionIT extends AbstractProjectionConstructorIT {

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void noArg() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}
		class MyProjection {
			public final DocumentReference documentReference;

			@ProjectionConstructor
			public MyProjection(@DocumentReferenceProjection DocumentReference documentReference) {
				this.documentReference = documentReference;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( reference( INDEX_NAME, "1" ) ),
						Arrays.asList( reference( INDEX_NAME, "2" ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.documentReference()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( reference( INDEX_NAME, "1" ) ),
						new MyProjection( reference( INDEX_NAME, "2" ) )
				)
		);
	}

	@Test
	public void supertype() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}
		class MyProjection {
			public final Object documentReference;

			@ProjectionConstructor
			public MyProjection(@DocumentReferenceProjection Object documentReference) {
				this.documentReference = documentReference;
			}
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MyProjection.class,
				Arrays.asList(
						Arrays.asList( reference( INDEX_NAME, "1" ) ),
						Arrays.asList( reference( INDEX_NAME, "2" ) )
				),
				f -> f.composite()
						.from(
								dummyProjectionForEnclosingClassInstance( f ),
								f.documentReference()
						)
						.asList(),
				Arrays.asList(
						new MyProjection( reference( INDEX_NAME, "1" ) ),
						new MyProjection( reference( INDEX_NAME, "2" ) )
				)
		);
	}

	@Test
	public void invalidType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}
		class MyProjection {
			public final Integer documentReference;

			@ProjectionConstructor
			public MyProjection(@DocumentReferenceProjection Integer documentReference) {
				this.documentReference = documentReference;
			}
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( MyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MyProjection.class.getName() )
						.constructorContext( ProjectionConstructorDocumentReferenceProjectionIT.class, Integer.class )
						.methodParameterContext( 1, "documentReference" )
						.failure(
								"Invalid projection definition for constructor parameter type '" + Integer.class.getName()
										+ "'",
								"This projection results in values of type '" + DocumentReference.class.getName() + "'" )
				);
	}

}
