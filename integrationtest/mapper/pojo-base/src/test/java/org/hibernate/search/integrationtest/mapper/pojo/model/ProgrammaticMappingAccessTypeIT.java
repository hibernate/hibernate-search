/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.model;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Integration tests checking that we get the expected access type for properties when using programmatic mapping,
 * and that the type of properties is inferred accordingly.
 * <p>
 * Similar to {@link AnnotationMappingAccessTypeIT}, which tests annotation mapping.
 */
public class ProgrammaticMappingAccessTypeIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "propertyWithGetterAndFieldDeclaredInParent", String.class )
				.field( "propertyWithFieldDeclaredInParent", String.class )
				.field( "propertyWithFieldDeclaredInParentThenGetterInChild", String.class )
				.field( "propertyWithGetterAndFieldDeclaredInParentThenOverridden", String.class )
				.field( "propertyWithGetterDeclaredAbstractInParent", String.class )
				.field( "propertyWithGetterAndField", String.class )
				.field( "propertyWithFieldOnly", String.class )
				.field( "propertyWithGetterAndDifferentlyNamedField", String.class )
		);

		mapping = setupHelper.start()
				.withConfiguration( b -> {
					ProgrammaticMappingConfigurationContext mapping = b.programmaticMapping();

					TypeMappingStep indexedEntityMapping = mapping.type( IndexedEntity.class );
					indexedEntityMapping.indexed().index( IndexedEntity.NAME );
					indexedEntityMapping.property( "id" ).documentId();
					indexedEntityMapping.property( "propertyWithGetterAndFieldDeclaredInParent" ).genericField();
					indexedEntityMapping.property( "propertyWithFieldDeclaredInParent" ).genericField();
					indexedEntityMapping.property( "propertyWithFieldDeclaredInParentThenGetterInChild" ).genericField();
					indexedEntityMapping.property( "propertyWithGetterAndFieldDeclaredInParentThenOverridden" ).genericField();
					indexedEntityMapping.property( "propertyWithGetterDeclaredAbstractInParent" ).genericField();
					indexedEntityMapping.property( "propertyWithGetterAndField" ).genericField();
					indexedEntityMapping.property( "propertyWithFieldOnly" ).genericField();
					indexedEntityMapping.property( "propertyWithGetterAndDifferentlyNamedField" ).genericField();
				} )
				.setup(
						IndexedEntity.class,
						ParentIndexedEntity.class
				);
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void index() {
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.id = 1;
			entity1.propertyWithGetterAndFieldDeclaredInParent = "propertyWithGetterAndFieldDeclaredInParent";
			entity1.propertyWithFieldDeclaredInParent = "propertyWithFieldDeclaredInParent";
			entity1.propertyWithFieldDeclaredInParentThenGetterInChild = "propertyWithFieldDeclaredInParentThenGetterInChild";
			entity1.propertyWithGetterAndFieldDeclaredInParentThenOverridden =
					"propertyWithGetterAndFieldDeclaredInParentThenOverridden";
			entity1.propertyWithGetterDeclaredAbstractInParent = "propertyWithGetterDeclaredAbstractInParent";
			entity1.propertyWithGetterAndField = "propertyWithGetterAndField";
			entity1.propertyWithFieldOnly = "propertyWithFieldOnly";
			entity1.internalFieldForPropertyWithGetterAndDifferentlyNamedField = "propertyWithGetterAndDifferentlyNamedField";

			session.indexingPlan().add( entity1 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.field( "propertyWithGetterAndFieldDeclaredInParent",
									entity1.propertyWithGetterAndFieldDeclaredInParent )
							.field( "propertyWithFieldDeclaredInParent", entity1.propertyWithFieldDeclaredInParent )
							.field( "propertyWithFieldDeclaredInParentThenGetterInChild",
									entity1.propertyWithFieldDeclaredInParentThenGetterInChild )
							.field( "propertyWithGetterAndFieldDeclaredInParentThenOverridden",
									entity1.propertyWithGetterAndFieldDeclaredInParentThenOverridden )
							.field( "propertyWithGetterDeclaredAbstractInParent",
									entity1.propertyWithGetterDeclaredAbstractInParent )
							.field( "propertyWithGetterAndField", entity1.propertyWithGetterAndField )
							.field( "propertyWithFieldOnly", entity1.propertyWithFieldOnly )
							.field( "propertyWithGetterAndDifferentlyNamedField",
									entity1.internalFieldForPropertyWithGetterAndDifferentlyNamedField )
					);
		}
	}

	public abstract static class ParentIndexedEntity {

		protected Object propertyWithGetterAndFieldDeclaredInParent;

		protected String propertyWithFieldDeclaredInParent;

		protected Object propertyWithFieldDeclaredInParentThenGetterInChild;

		protected Object propertyWithGetterAndFieldDeclaredInParentThenOverridden;

		public String getPropertyWithGetterAndFieldDeclaredInParent() {
			return (String) propertyWithGetterAndFieldDeclaredInParent;
		}

		public abstract String getPropertyWithGetterDeclaredAbstractInParent();

		public Object getPropertyWithGetterAndFieldDeclaredInParentThenOverridden() {
			return propertyWithGetterAndFieldDeclaredInParentThenOverridden;
		}
	}

	public static class IndexedEntity extends ParentIndexedEntity {

		public static final String NAME = "IndexedEntity";

		private Object id;

		protected Object propertyWithGetterDeclaredAbstractInParent;

		protected Object propertyWithGetterAndField;

		protected String propertyWithFieldOnly;

		protected Object internalFieldForPropertyWithGetterAndDifferentlyNamedField;

		public Integer getId() {
			return (Integer) id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getPropertyWithFieldDeclaredInParentThenGetterInChild() {
			return (String) propertyWithFieldDeclaredInParentThenGetterInChild;
		}

		@Override
		public String getPropertyWithGetterDeclaredAbstractInParent() {
			return (String) propertyWithGetterDeclaredAbstractInParent;
		}

		@Override
		public String getPropertyWithGetterAndFieldDeclaredInParentThenOverridden() {
			return (String) super.getPropertyWithGetterAndFieldDeclaredInParentThenOverridden();
		}

		public String getPropertyWithGetterAndField() {
			return (String) propertyWithGetterAndField;
		}

		public String getPropertyWithGetterAndDifferentlyNamedField() {
			return (String) internalFieldForPropertyWithGetterAndDifferentlyNamedField;
		}
	}

}
