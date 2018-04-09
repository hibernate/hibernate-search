/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.dirtiness.building.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerValueExtractorBinder;
import org.hibernate.search.mapper.pojo.model.augmented.building.impl.PojoAugmentedTypeModelProvider;
import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAugmentedPropertyModel;
import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAugmentedTypeModel;
import org.hibernate.search.mapper.pojo.model.augmented.impl.PojoAugmentedValueModel;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.SearchException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;

public class PojoAssociationPathInverterTest extends EasyMockSupport {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final PojoAugmentedTypeModelProvider augmentedTypeModelProviderMock =
			createMock( PojoAugmentedTypeModelProvider.class );
	private final PojoBootstrapIntrospector introspectorMock =
			createMock( PojoBootstrapIntrospector.class );
	private final ContainerValueExtractorBinder extractorBinderMock =
			createMock( ContainerValueExtractorBinder.class );

	@Test
	public void detectInfiniteRecursion() {
		String originalSidePropertyName = "originalSideProperty";
		String inverseSideProperty1Name = "inverseSideProperty1";
		String inverseSideProperty2Name = "inverseSideProperty2";
		String inverseSideProperty3Name = "inverseSideProperty3";

		PojoAssociationPathInverter inverter =
				new PojoAssociationPathInverter( augmentedTypeModelProviderMock, introspectorMock, extractorBinderMock );

		resetAll();
		/*
		 * Original side model:
		 *
		 * - type
		 * -- property
		 */
		PojoRawTypeModel<?> originalSideEntityTypeMock =
				createMock( "originalSideEntityTypeMock", PojoRawTypeModel.class );
		EasyMock.expect( originalSideEntityTypeMock.getRawType() )
				.andStubReturn( (PojoRawTypeModel) originalSideEntityTypeMock );

		PojoGenericTypeModel<?> originalSidePropertyTypeMock =
				createMock( "originalSidePropertyTypeMock", PojoGenericTypeModel.class );
		PropertyHandle originalSidePropertyHandleMock = setupPropertyStub(
				originalSideEntityTypeMock, originalSidePropertyName, originalSidePropertyTypeMock
		);

		PojoAugmentedTypeModel originalSideAugmentedEntityTypeMock =
				createMock( "originalSideAugmentedEntityTypeMock", PojoAugmentedTypeModel.class );
		PojoAugmentedPropertyModel originalSideAugmentedPropertyMock =
				createMock( "originalSideAugmentedPropertyMock", PojoAugmentedPropertyModel.class );
		PojoAugmentedValueModel originalSideAugmentedValueMock =
				createMock( "originalSideAugmentedValueMock", PojoAugmentedValueModel.class );
		EasyMock.expect( augmentedTypeModelProviderMock.get( originalSideEntityTypeMock ) )
				.andStubReturn( originalSideAugmentedEntityTypeMock );
		EasyMock.expect( originalSideAugmentedEntityTypeMock.getProperty( originalSidePropertyName ) )
				.andStubReturn( originalSideAugmentedPropertyMock );
		EasyMock.expect( originalSideAugmentedPropertyMock.getValue( ContainerValueExtractorPath.noExtractors() ) )
				.andStubReturn( originalSideAugmentedValueMock );
		EasyMock.expect( originalSideAugmentedValueMock.getInverseSidePath() )
				.andStubReturn( Optional.empty() );

		/*
		 * Inverse side model:
		 *
		 * - type
		 * -- property1 (embeddableType1)
		 * ---- property2 (embeddableType2)
		 * ------ property3 (embeddableType1) <- CYCLE
		 */
		PojoRawTypeModel<?> inverseSideEntityTypeMock =
				createMock( "inverseSideEntityTypeMock", PojoRawTypeModel.class );
		PojoRawTypeModel<?> inverseSideEmbeddableType1Mock =
				createMock( "inverseSideEmbeddableType1Mock", PojoRawTypeModel.class );
		PojoRawTypeModel<?> inverseSideEmbeddableType2Mock =
				createMock( "inverseSideEmbeddableType2Mock", PojoRawTypeModel.class );
		EasyMock.expect( inverseSideEntityTypeMock.getRawType() )
				.andStubReturn( (PojoRawTypeModel) inverseSideEntityTypeMock );

		PojoGenericTypeModel<?> inverseSideProperty1TypeMock =
				createMock( "inverseSideProperty1TypeMock", PojoGenericTypeModel.class );
		setupPropertyStub(
				inverseSideEntityTypeMock, inverseSideProperty1Name, inverseSideProperty1TypeMock
		);
		EasyMock.expect( inverseSideProperty1TypeMock.getRawType() )
				.andStubReturn( (PojoRawTypeModel) inverseSideEmbeddableType1Mock );

		PojoGenericTypeModel<?> inverseSideProperty2TypeMock =
				createMock( "inverseSideProperty2TypeMock", PojoGenericTypeModel.class );
		setupPropertyStub(
				inverseSideProperty1TypeMock, inverseSideProperty2Name, inverseSideProperty2TypeMock
		);
		EasyMock.expect( inverseSideProperty2TypeMock.getRawType() )
				.andStubReturn( (PojoRawTypeModel) inverseSideEmbeddableType2Mock );

		PojoGenericTypeModel<?> inverseSideProperty3TypeMock =
				createMock( "inverseSideProperty3TypeMock", PojoGenericTypeModel.class );
		setupPropertyStub(
				inverseSideProperty2TypeMock, inverseSideProperty3Name, inverseSideProperty2TypeMock
		);
		EasyMock.expect( inverseSideProperty3TypeMock.getRawType() )
				.andStubReturn( (PojoRawTypeModel) inverseSideEmbeddableType1Mock );

		PojoAugmentedTypeModel inverseSideAugmentedEntityTypeMock =
				createMock( "inverseSideAugmentedEntityTypeMock", PojoAugmentedTypeModel.class );
		EasyMock.expect( augmentedTypeModelProviderMock.get( inverseSideEntityTypeMock ) )
				.andStubReturn( inverseSideAugmentedEntityTypeMock );
		setupSingletonEmbeddedAugmentedPropertiesStub( inverseSideAugmentedEntityTypeMock, inverseSideProperty1Name );

		PojoAugmentedTypeModel inverseSideAugmentedEmbeddableType1Mock =
				createMock( "inverseSideAugmentedEmbeddableType1Mock", PojoAugmentedTypeModel.class );
		EasyMock.expect( augmentedTypeModelProviderMock.get( inverseSideEmbeddableType1Mock ) )
				.andStubReturn( inverseSideAugmentedEmbeddableType1Mock );
		setupSingletonEmbeddedAugmentedPropertiesStub( inverseSideAugmentedEmbeddableType1Mock, inverseSideProperty2Name );

		PojoAugmentedTypeModel inverseSideAugmentedEmbeddableType2Mock =
				createMock( "inverseSideAugmentedEmbeddableType2Mock", PojoAugmentedTypeModel.class );
		EasyMock.expect( augmentedTypeModelProviderMock.get( inverseSideEmbeddableType2Mock ) )
				.andStubReturn( inverseSideAugmentedEmbeddableType2Mock );
		setupSingletonEmbeddedAugmentedPropertiesStub( inverseSideAugmentedEmbeddableType2Mock, inverseSideProperty3Name );

		// Let's not complicate things any further: assume that none of the paths is the default one
		EasyMock.expect( extractorBinderMock.tryBindPath(
				EasyMock.eq( introspectorMock ), EasyMock.anyObject(), EasyMock.anyObject()
		) )
				.andStubReturn( Optional.empty() );
		// Let's not complicate things any further: assume that all extractor paths are noExtractors() paths
		EasyMock.expect( extractorBinderMock.bindPath(
				EasyMock.eq( introspectorMock ), EasyMock.anyObject(),
				EasyMock.eq( ContainerValueExtractorPath.noExtractors() )
		) )
				.andStubAnswer( (IAnswer) () -> {
					PojoGenericTypeModel<?> sourceType =
							(PojoGenericTypeModel<?>) EasyMock.getCurrentArguments()[1];
					return BoundContainerValueExtractorPath.noExtractors( sourceType );
				} );

		replayAll();
		BoundPojoModelPathValueNode<?, ?, ?> boundPathToInvert =
				BoundPojoModelPathValueNode.root( originalSideEntityTypeMock )
						.property( originalSidePropertyHandleMock )
						.value( (BoundContainerValueExtractorPath) BoundContainerValueExtractorPath.noExtractors(
								originalSidePropertyTypeMock
						) );
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Found an infinite embedded recursion involving path '"
				+ PojoModelPath.fromRoot( inverseSideProperty1Name ).value( ContainerValueExtractorPath.noExtractors() )
						.property( inverseSideProperty2Name ).value( ContainerValueExtractorPath.noExtractors() )
						.property( inverseSideProperty3Name ).value( ContainerValueExtractorPath.noExtractors() )
				+ "' on type '" + inverseSideEntityTypeMock + "'" );
		try {
			inverter.invertPath( inverseSideEntityTypeMock, boundPathToInvert );
		}
		finally {
			verifyAll();
		}
	}

	private PropertyHandle setupPropertyStub(PojoTypeModel<?> holdingTypeMock, String propertyName,
			PojoGenericTypeModel<?> propertyTypeMock) {
		PropertyHandle propertyHandleMock =
				createMock( propertyName + "HandleMock", PropertyHandle.class );
		PojoPropertyModel<?> propertyModelMock =
				createMock( propertyName + "ModelMock", PojoPropertyModel.class );
		EasyMock.expect( holdingTypeMock.getProperty( propertyName ) )
				.andStubReturn( (PojoPropertyModel) propertyModelMock );
		EasyMock.expect( propertyHandleMock.getName() ).andStubReturn( propertyName );
		EasyMock.expect( propertyModelMock.getName() ).andStubReturn( propertyName );
		EasyMock.expect( propertyModelMock.getHandle() ).andStubReturn( propertyHandleMock );
		EasyMock.expect( propertyModelMock.getTypeModel() )
				.andStubReturn( (PojoGenericTypeModel) propertyTypeMock );
		return propertyHandleMock;
	}

	private void setupSingletonEmbeddedAugmentedPropertiesStub(PojoAugmentedTypeModel augmentedTypeModel, String propertyName) {
		PojoAugmentedPropertyModel augmentedPropertyModelMock =
				createMock( propertyName + "AugmentedPropertyModelMock", PojoAugmentedPropertyModel.class );
		PojoAugmentedValueModel augmentedValueModelMock =
				createMock( propertyName + "AugmentedValueModelMock", PojoAugmentedValueModel.class );

		Map<String, PojoAugmentedPropertyModel> properties = new HashMap<>();
		properties.put( propertyName, augmentedPropertyModelMock );
		Map<ContainerValueExtractorPath, PojoAugmentedValueModel> values = new HashMap<>();
		values.put( ContainerValueExtractorPath.noExtractors(), augmentedValueModelMock );

		EasyMock.expect( augmentedTypeModel.getAugmentedProperties() )
				.andStubReturn( properties );
		EasyMock.expect( augmentedPropertyModelMock.getAugmentedValues() )
				.andStubReturn( values );
		EasyMock.expect( augmentedValueModelMock.getInverseSidePath() )
				.andStubReturn( Optional.empty() );
		EasyMock.expect( augmentedValueModelMock.isAssociationEmbedded() )
				.andStubReturn( true );
	}

}
