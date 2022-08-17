/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * See https://discourse.hibernate.org/t/propertybinder-for-base-type-reports-no-children-property-found-in-parent-class/5493/2
 */
@TestForIssue(jiraKey = "HSEARCH-4491")
public class PropertyBridgeRuntimePolymorphismHackIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock(
			MethodHandles.lookup(), backendMock );

	@Test
	public void explicitReindexing_hack_runtimePolymorphism() {
		backendMock.expectSchema( INDEX_NAME, b ->
				b.field( "someField", String.class ) );

		SearchMapping mapping = setupHelper.start()
				.setup( IndexedEntity.class, AbstractContainedEntity.class, ContainedEntity1.class );
		backendMock.verifyExpectationsMet();

		IndexedEntity indexed1 = new IndexedEntity();
		indexed1.id = 1;
		ContainedEntity1 contained1 = new ContainedEntity1();
		indexed1.contained = contained1;
		contained1.containing = indexed1;
		contained1.stringProperty = "initial value";

		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlan().add( indexed1 );
			session.indexingPlan().add( 1, null, contained1 );

			backendMock.expectWorks( INDEX_NAME )
					.add( "1", b -> b.field( "someField", "initial value" ) );
		}
		backendMock.verifyExpectationsMet();

		try ( SearchSession session = mapping.createSession() ) {
			contained1.stringProperty = "updated value";

			session.indexingPlan().addOrUpdate( 1, null, contained1, "stringProperty" );

			backendMock.expectWorks( INDEX_NAME )
					.addOrUpdate( "1", b -> b.field( "someField", "updated value" ) );
		}
		backendMock.verifyExpectationsMet();

		IndexedEntity indexed2 = new IndexedEntity();
		indexed2.id = 1;
		ContainedEntity2 contained2 = new ContainedEntity2();
		indexed2.contained = contained2;
		contained2.containing = indexed2;
		contained2.otherProperty = "initial value";

		try ( SearchSession session = mapping.createSession() ) {
			contained2.otherProperty = "updated value";

			// There is no dependency from IndexedEntity to ContainedEntity2
			assertThatThrownBy( () -> session.indexingPlan()
					.addOrUpdate( 2, null, contained2, "otherProperty" ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "the entity type is not mapped in Hibernate Search" );
		}
		backendMock.verifyExpectationsMet();
	}

	@Indexed(index = INDEX_NAME)
	static class IndexedEntity {
		@DocumentId
		Integer id;
		@PropertyBinding(binder = @PropertyBinderRef(type = PropertyBinderWithRuntimePolymorphism.class,
				// Only necessary because of constraints of our testing framework
				retrieval = BeanRetrieval.CONSTRUCTOR))
		@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "containing")))
		AbstractContainedEntity contained;
	}

	abstract static class AbstractContainedEntity {
		IndexedEntity containing;
		@DocumentId
		Integer id;

		public abstract AbstractContainedEntity getSelf();
	}

	static class ContainedEntity1 extends AbstractContainedEntity {
		String stringProperty;

		@Override
		public ContainedEntity1 getSelf() {
			return this;
		}
	}

	static class ContainedEntity2 extends AbstractContainedEntity {
		String otherProperty;

		@Override
		public ContainedEntity2 getSelf() {
			return this;
		}
	}

	public static class PropertyBinderWithRuntimePolymorphism implements PropertyBinder {
		@Override
		public void bind(PropertyBindingContext context) {
			context.dependencies()
					.fromOtherEntity( ContainedEntity1.class, "self" )
					.use( "stringProperty" );
			IndexFieldReference<String> indexFieldReference =
					context.indexSchemaElement().field( "someField", f -> f.asString() )
							.toReference();
			context.bridge( AbstractContainedEntity.class, new Bridge( indexFieldReference ) );
		}

		private static class Bridge implements PropertyBridge<AbstractContainedEntity> {
			private final IndexFieldReference<String> indexFieldReference;

			public Bridge(IndexFieldReference<String> indexFieldReference) {
				this.indexFieldReference = indexFieldReference;
			}

			@Override
			public void write(DocumentElement target,
					AbstractContainedEntity bridgedElement,
					PropertyBridgeWriteContext context1) {
				if ( bridgedElement instanceof ContainedEntity1 ) {
					target.addValue(
							indexFieldReference,
							( (ContainedEntity1) bridgedElement ).stringProperty
					);
				}
			}
		}
	}
}
