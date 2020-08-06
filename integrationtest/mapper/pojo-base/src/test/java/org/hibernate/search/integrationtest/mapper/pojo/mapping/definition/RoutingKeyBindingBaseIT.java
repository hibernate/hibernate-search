/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBindingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test common use cases of the {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.RoutingKeyBinding} annotation.
 */
@SuppressWarnings({"unused", "deprecation"})
@TestForIssue(jiraKey = "HSEARCH-3135")
public class RoutingKeyBindingBaseIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	/**
	 * Basic test checking that a simple type binding will be applied as expected.
	 */
	@Test
	public void simple() {
		backendMock.expectSchema( INDEX_NAME, b -> b
				.explicitRouting()
		);

		SearchMapping mapping = setupHelper.start()
				.setup( IndexedEntityWithWorkingRoutingKeyBinding.class );
		backendMock.verifyExpectationsMet();
	}

	@Indexed(index = INDEX_NAME)
	@org.hibernate.search.mapper.pojo.mapping.definition.annotation.RoutingKeyBinding(
			binder = @org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingKeyBinderRef(type = WorkingRoutingKeyBinder.class))
	private static class IndexedEntityWithWorkingRoutingKeyBinding {
		Integer id;
		@DocumentId
		public Integer getId() {
			return id;
		}
	}

	public static class WorkingRoutingKeyBinder implements org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder {
		@Override
		public void bind(RoutingKeyBindingContext context) {
			context.dependencies().useRootOnly();
			context.bridge( (String tenantIdentifier, Object entityIdentifier, Object bridgedElement,
					org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext context1) -> {
				throw new UnsupportedOperationException( "Should not be called " );
			} );
		}
	}

	@Test
	public void missingBinderReference() {
		@Indexed
		@org.hibernate.search.mapper.pojo.mapping.definition.annotation.RoutingKeyBinding(
				binder = @org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingKeyBinderRef)
		class IndexedEntity {
			Integer id;
			@DocumentId
			public Integer getId() {
				return id;
			}
		}
		Assertions.assertThatThrownBy(
				() -> setupHelper.start().setup( IndexedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( IndexedEntity.class.getName() )
						.annotationContextAnyParameters( org.hibernate.search.mapper.pojo.mapping.definition.annotation.RoutingKeyBinding.class )
						.failure( "The binder reference is empty." )
						.build()
				);
	}
}
