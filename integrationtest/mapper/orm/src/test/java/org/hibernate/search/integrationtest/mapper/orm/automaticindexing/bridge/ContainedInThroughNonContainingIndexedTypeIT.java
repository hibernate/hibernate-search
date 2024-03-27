/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.bridge;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests a corner case that is not covered by
 * {@link AutomaticIndexingBridgeAccessorsIT},
 * where the contained entity type is also indexed.
 * This should not matter given the current implementation, but better safe than sorry.
 */
@TestForIssue(jiraKey = "HSEARCH-2496")
class ContainedInThroughNonContainingIndexedTypeIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		backendMock.expectAnySchema( Containing.INDEX );
		backendMock.expectAnySchema( Contained.INDEX );

		sessionFactory = ormSetupHelper.start()
				.setup(
						Containing.class,
						Contained.class
				);
		backendMock.verifyExpectationsMet();
	}

	@Test
	void test() {
		with( sessionFactory ).runInTransaction( session -> {
			Containing containing = new Containing();
			containing.setId( 1 );

			Contained contained = new Contained();
			contained.setId( 2 );
			containing.setContained( contained );
			contained.setContaining( containing );

			session.persist( contained );
			session.persist( containing );

			backendMock.expectWorks( Containing.INDEX )
					.add( "1", b -> b
							.field( "indexedInContaining", 0 )
					);

			backendMock.expectWorks( Contained.INDEX )
					.add( "2", b -> b
							.field( "indexedInContained", 0 )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		with( sessionFactory ).runInTransaction( session -> {
			Contained contained = session.get( Contained.class, 2 );
			contained.setIndexedInContaining( 42 );

			/*
			 * We expect the index for the Containing entity to be updated,
			 * even though the mapping for the contained entity does not depend on
			 * the modified property.
			 */
			backendMock.expectWorks( Containing.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "indexedInContaining", 42 )
					);

			// No work is expected on the index for the Contained entity
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = Containing.INDEX)
	@Indexed(index = Containing.INDEX)
	public static class Containing {
		static final String INDEX = "Containing";

		@Id
		private Integer id;

		@PropertyBinding(binder = @PropertyBinderRef(type = BridgeGoingThroughEntityBoundaries.Binder.class))
		@OneToOne
		private Contained contained;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Contained getContained() {
			return contained;
		}

		public void setContained(
				Contained contained) {
			this.contained = contained;
		}
	}

	@Entity(name = Contained.INDEX)
	@Indexed(index = Contained.INDEX)
	public static class Contained {
		static final String INDEX = "Contained";

		@Id
		private Integer id;

		@OneToOne(mappedBy = "contained")
		private Containing containing;

		@GenericField
		private int indexedInContained;

		/*
		 * This property is considered irrelevant when doing dirty-checking on Contained,
		 * because it's not indexed as far as Contained is concerned.
		 * The property is used when indexing Containing, though, but its dirtiness is being
		 * ignored anyway...
		 */
		private int indexedInContaining;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Containing getContaining() {
			return containing;
		}

		public void setContaining(
				Containing containing) {
			this.containing = containing;
		}

		public int getIndexedInContained() {
			return indexedInContained;
		}

		public void setIndexedInContained(int indexedInContained) {
			this.indexedInContained = indexedInContained;
		}

		public int getIndexedInContaining() {
			return indexedInContaining;
		}

		public void setIndexedInContaining(int indexedInContaining) {
			this.indexedInContaining = indexedInContaining;
		}
	}

	public static class BridgeGoingThroughEntityBoundaries implements PropertyBridge<Contained> {

		private IndexFieldReference<Integer> indexFieldReference;

		private BridgeGoingThroughEntityBoundaries(PropertyBindingContext context) {
			context.dependencies().use( "indexedInContaining" );

			indexFieldReference = context.indexSchemaElement().field(
					"indexedInContaining", f -> f.asInteger()
			)
					.toReference();
		}

		@Override
		public void write(DocumentElement target, Contained bridgedElement, PropertyBridgeWriteContext context) {
			Integer value = bridgedElement.getIndexedInContaining();
			target.addValue( indexFieldReference, value );
		}

		public static class Binder implements PropertyBinder {
			@Override
			public void bind(PropertyBindingContext context) {
				context.bridge( Contained.class, new BridgeGoingThroughEntityBoundaries( context ) );
			}
		}
	}
}
