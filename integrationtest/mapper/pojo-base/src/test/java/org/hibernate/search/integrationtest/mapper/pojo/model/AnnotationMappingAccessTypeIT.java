/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.model;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Integration tests checking that we get the expected access type for properties when using annotation mapping,
 * and that the type of properties is inferred accordingly.
 * <p>
 * Similar to {@link ProgrammaticMappingAccessTypeIT}, which tests programmatic mapping.
 */
class AnnotationMappingAccessTypeIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@BeforeEach
	void setup() {
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
				.setup(
						IndexedEntity.class,
						ParentIndexedEntity.class
				);
		backendMock.verifyExpectationsMet();
	}

	@Test
	void index() {
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

		@GenericField
		protected Object propertyWithGetterAndFieldDeclaredInParent;

		@GenericField
		protected String propertyWithFieldDeclaredInParent;

		@GenericField
		protected Object propertyWithFieldDeclaredInParentThenGetterInChild;

		@GenericField
		protected Object propertyWithGetterAndFieldDeclaredInParentThenOverridden;

		public String getPropertyWithGetterAndFieldDeclaredInParent() {
			return (String) propertyWithGetterAndFieldDeclaredInParent;
		}

		public abstract String getPropertyWithGetterDeclaredAbstractInParent();

		public Object getPropertyWithGetterAndFieldDeclaredInParentThenOverridden() {
			return propertyWithGetterAndFieldDeclaredInParentThenOverridden;
		}
	}

	@Indexed(index = IndexedEntity.NAME)
	public static class IndexedEntity extends ParentIndexedEntity {

		public static final String NAME = "IndexedEntity";

		@DocumentId
		private Object id;

		@GenericField
		protected Object propertyWithGetterDeclaredAbstractInParent;

		@GenericField
		protected Object propertyWithGetterAndField;

		@GenericField
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

		@GenericField
		public String getPropertyWithGetterAndDifferentlyNamedField() {
			return (String) internalFieldForPropertyWithGetterAndDifferentlyNamedField;
		}
	}

}
