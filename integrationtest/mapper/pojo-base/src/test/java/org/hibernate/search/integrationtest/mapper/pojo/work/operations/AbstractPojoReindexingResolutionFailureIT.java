/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

abstract class AbstractPojoReindexingResolutionFailureIT {

	@Rule
	public final BackendMock backendMock = new BackendMock();

	@Rule
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	protected SearchMapping mapping;

	@Before
	public void setup() {
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
	public void getter() {
		NonRootEntity level1 = new NonRootEntity();

		SimulatedFailure simulatedFailure = new SimulatedFailure();
		level1.exceptionOnGetterCall = simulatedFailure;

		try ( SearchSession session = mapping.createSessionWithOptions().build() ) {
			assertThatThrownBy( () -> process( session, level1 ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( SimulatedFailure.MESSAGE, ".containingNoContainer" )
					.hasRootCause( simulatedFailure );
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void containerExtraction() {
		NonRootEntity level1 = new NonRootEntity();
		level1.containingInContainer = Mockito.mock( List.class );

		SimulatedFailure simulatedFailure = new SimulatedFailure();
		when( level1.containingInContainer.iterator() ).thenThrow( simulatedFailure );

		try ( SearchSession session = mapping.createSessionWithOptions().build() ) {
			assertThatThrownBy( () -> process( session, level1 ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( SimulatedFailure.MESSAGE, ".containingInContainer" )
					.hasRootCause( simulatedFailure );
		}
	}

	@Test
	public void nested_getter() {
		NonRootEntity level1 = new NonRootEntity();

		SimulatedFailure simulatedFailure = new SimulatedFailure();
		level1.exceptionOnGetterCall = simulatedFailure;

		NonRootEntity level2 = new NonRootEntity();
		level2.containingNoContainer = level1;

		try ( SearchSession session = mapping.createSessionWithOptions().build() ) {
			assertThatThrownBy( () -> process( session, level2 ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( SimulatedFailure.MESSAGE,
							".containingNoContainer<no value extractors>.containingNoContainer" )
					.hasRootCause( simulatedFailure );
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void nested_containerExtraction() {
		NonRootEntity level1 = new NonRootEntity();
		level1.containingInContainer = Mockito.mock( List.class );

		SimulatedFailure simulatedFailure = new SimulatedFailure();
		when( level1.containingInContainer.iterator() ).thenThrow( simulatedFailure );

		NonRootEntity level2 = new NonRootEntity();
		level2.containingNoContainer = level1;

		try ( SearchSession session = mapping.createSessionWithOptions().build() ) {
			assertThatThrownBy( () -> process( session, level2 ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( SimulatedFailure.MESSAGE,
							".containingNoContainer<no value extractors>.containingInContainer" )
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
	}

	@Indexed(index = RootEntity.NAME)
	static class RootEntity extends AbstractEntity {
		public static final String NAME = "RootEntity";
	}

	static class NonRootEntity extends AbstractEntity {
		AbstractEntity containingNoContainer;

		List<AbstractEntity> containingInContainer;

		RuntimeException exceptionOnGetterCall = null;

		public AbstractEntity getContainingNoContainer() {
			if ( exceptionOnGetterCall != null ) {
				throw exceptionOnGetterCall;
			}
			return containingNoContainer;
		}
	}


}
