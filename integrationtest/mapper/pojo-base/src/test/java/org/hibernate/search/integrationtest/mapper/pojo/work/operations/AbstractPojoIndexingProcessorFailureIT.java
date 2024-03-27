/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
abstract class AbstractPojoIndexingProcessorFailureIT {

	@RegisterExtension
	public final BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	protected SearchMapping mapping;

	@BeforeEach
	void setup() {
		backendMock.expectSchema( RootEntity.NAME, b -> b
				.field( "value", String.class )
				.objectField( "containedNoContainer", b2 -> b2
						.field( "value", String.class )
						.objectField( "containedNoContainer", b3 -> b3
								.field( "value", String.class ) )
						.objectField( "containedInContainer", b3 -> b3
								.multiValued( true )
								.field( "value", String.class ) ) )
				.objectField( "containedInContainer", b2 -> b2
						.multiValued( true )
						.field( "value", String.class )
						.objectField( "containedNoContainer", b3 -> b3
								.field( "value", String.class ) )
						.objectField( "containedInContainer", b3 -> b3
								.multiValued( true )
								.field( "value", String.class ) ) ) );

		mapping = setupHelper.start().setup( RootEntity.class, NonRootEntity.class );

		backendMock.verifyExpectationsMet();
	}

	@Test
	void getter() {
		RootEntity root = new RootEntity();
		root.id = 1;

		SimulatedFailure simulatedFailure = new SimulatedFailure();
		root.exceptionOnGetterCall = simulatedFailure;

		try ( SearchSession session = mapping.createSessionWithOptions().build() ) {
			assertThatThrownBy( () -> process( session, root ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( SimulatedFailure.MESSAGE, ".containedNoContainer" )
					.hasRootCause( simulatedFailure );
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void containerExtraction() {
		RootEntity root = new RootEntity();
		root.id = 1;
		root.containedInContainer = Mockito.mock( List.class );

		SimulatedFailure simulatedFailure = new SimulatedFailure();
		when( root.containedInContainer.iterator() ).thenThrow( simulatedFailure );

		try ( SearchSession session = mapping.createSessionWithOptions().build() ) {
			assertThatThrownBy( () -> process( session, root ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( SimulatedFailure.MESSAGE, ".containedInContainer" )
					.hasRootCause( simulatedFailure );
		}
	}

	@Test
	void nested_getter() {
		RootEntity root = new RootEntity();
		root.id = 1;
		NonRootEntity level1 = new NonRootEntity();
		root.containedNoContainer = level1;

		SimulatedFailure simulatedFailure = new SimulatedFailure();
		level1.exceptionOnGetterCall = simulatedFailure;

		try ( SearchSession session = mapping.createSessionWithOptions().build() ) {
			assertThatThrownBy( () -> process( session, root ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( SimulatedFailure.MESSAGE,
							".containedNoContainer<no value extractors>.containedNoContainer" )
					.hasRootCause( simulatedFailure );
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void nested_containerExtraction() {
		RootEntity root = new RootEntity();
		root.id = 1;
		NonRootEntity level1 = new NonRootEntity();
		root.containedNoContainer = level1;
		level1.containedInContainer = Mockito.mock( List.class );

		SimulatedFailure simulatedFailure = new SimulatedFailure();
		when( level1.containedInContainer.iterator() ).thenThrow( simulatedFailure );

		try ( SearchSession session = mapping.createSessionWithOptions().build() ) {
			assertThatThrownBy( () -> process( session, root ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( SimulatedFailure.MESSAGE,
							".containedNoContainer<no value extractors>.containedInContainer" )
					.hasRootCause( simulatedFailure );
		}
	}

	protected abstract void process(SearchSession session, Object entity);

	static class SimulatedFailure extends RuntimeException {
		static final String MESSAGE = "Simulated failure";

		SimulatedFailure() {
			super( MESSAGE );
		}
	}

	static class AbstractEntity {
		@DocumentId
		Integer id;

		@GenericField
		String value;

		@IndexedEmbedded(includeDepth = 2)
		@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "containingNoContainer")))
		NonRootEntity containedNoContainer;

		@IndexedEmbedded(includeDepth = 2)
		@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "containingInContainer")))
		List<NonRootEntity> containedInContainer;

		RuntimeException exceptionOnGetterCall = null;

		public NonRootEntity getContainedNoContainer() {
			if ( exceptionOnGetterCall != null ) {
				throw exceptionOnGetterCall;
			}
			return containedNoContainer;
		}
	}

	@Indexed(index = RootEntity.NAME)
	static class RootEntity extends AbstractEntity {
		public static final String NAME = "RootEntity";
	}

	static class NonRootEntity extends AbstractEntity {
		AbstractEntity containingNoContainer;

		List<AbstractEntity> containingInContainer;
	}


}
