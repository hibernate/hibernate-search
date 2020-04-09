/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@link PropertyBinding} annotation.
 */
@SuppressWarnings("unused")
@TestForIssue(jiraKey = "HSEARCH-3135")
public class PropertyBindingBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	/**
	 * Basic test checking that a simple type binding will be applied as expected.
	 */
	@Test
	public void simple() {
		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "myText", String.class )
		);

		SearchMapping mapping = setupHelper.start()
				.setup( IndexedEntityWithWorkingPropertyBinding.class );
		backendMock.verifyExpectationsMet();
	}

	@Indexed(index = INDEX_NAME)
	private static class IndexedEntityWithWorkingPropertyBinding {
		Integer id;
		String text;
		@DocumentId
		public Integer getId() {
			return id;
		}
		@PropertyBinding(binder = @PropertyBinderRef(type = WorkingPropertyBinder.class))
		public String getText() {
			return text;
		}
	}

	public static class WorkingPropertyBinder implements PropertyBinder {
		@Override
		public void bind(PropertyBindingContext context) {
			context.getDependencies().useRootOnly();
			IndexFieldReference<String> indexFieldReference =
					context.getIndexSchemaElement().field( "myText", f -> f.asString() )
							.toReference();
			context.setBridge( (DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context1) -> {
				IndexedEntityWithWorkingPropertyBinding castedBridgedElement = (IndexedEntityWithWorkingPropertyBinding) bridgedElement;
				target.addValue(
						indexFieldReference, castedBridgedElement.text
				);
			} );
		}
	}

	@Test
	public void missingBinderReference() {
		@Indexed
		class IndexedEntity {
			Integer id;
			String text;
			@DocumentId
			public Integer getId() {
				return id;
			}
			@PropertyBinding(binder = @PropertyBinderRef)
			public String getText() {
				return text;
			}
		}
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.pathContext( ".text" )
						.annotationContextAnyParameters( PropertyBinding.class )
						.failure( "The binder reference is empty." )
						.build()
				);
	}
}
