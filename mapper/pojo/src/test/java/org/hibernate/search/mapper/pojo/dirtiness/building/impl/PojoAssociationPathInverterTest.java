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

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoPropertyAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoValueAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;
import org.hibernate.search.util.common.SearchException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;

@SuppressWarnings({"unchecked", "rawtypes"})
public class PojoAssociationPathInverterTest extends EasyMockSupport {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProviderMock =
			createMock( PojoTypeAdditionalMetadataProvider.class );
	private final ContainerExtractorBinder extractorBinderMock =
			createMock( ContainerExtractorBinder.class );

	@Test
	public void detectInfiniteRecursion() {
		String originalSidePropertyName = "originalSideProperty";
		String inverseSideProperty1Name = "inverseSideProperty1";
		String inverseSideProperty2Name = "inverseSideProperty2";
		String inverseSideProperty3Name = "inverseSideProperty3";

		PojoAssociationPathInverter inverter =
				new PojoAssociationPathInverter( typeAdditionalMetadataProviderMock, extractorBinderMock );

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
		setupPropertyStub(
				originalSideEntityTypeMock, originalSidePropertyName, originalSidePropertyTypeMock
		);

		PojoTypeAdditionalMetadata originalSideEntityTypeAdditionalMetadataMock =
				createMock( "originalSideEntityTypeAdditionalMetadataMock", PojoTypeAdditionalMetadata.class );
		PojoPropertyAdditionalMetadata originalSidePropertyAdditionalMetadataMock =
				createMock( "originalSidePropertyAdditionalMetadataMock", PojoPropertyAdditionalMetadata.class );
		PojoValueAdditionalMetadata originalSideValueAdditionalMetadataMock =
				createMock( "originalSideValueAdditionalMetadataMock", PojoValueAdditionalMetadata.class );
		EasyMock.expect( typeAdditionalMetadataProviderMock.get( originalSideEntityTypeMock ) )
				.andStubReturn( originalSideEntityTypeAdditionalMetadataMock );
		EasyMock.expect( originalSideEntityTypeAdditionalMetadataMock.getPropertyAdditionalMetadata( originalSidePropertyName ) )
				.andStubReturn( originalSidePropertyAdditionalMetadataMock );
		EasyMock.expect( originalSidePropertyAdditionalMetadataMock.getValueAdditionalMetadata( ContainerExtractorPath.noExtractors() ) )
				.andStubReturn( originalSideValueAdditionalMetadataMock );
		EasyMock.expect( originalSideValueAdditionalMetadataMock.getInverseSidePath() )
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
		EasyMock.expect( inverseSideEntityTypeMock.getName() )
				.andStubReturn( "inverseSideEntityTypeMock" );

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

		PojoTypeAdditionalMetadata inverseSideEntityTypeAdditionalMetadataMock =
				createMock( "inverseSideEntityTypeAdditionalMetadataMock", PojoTypeAdditionalMetadata.class );
		EasyMock.expect( typeAdditionalMetadataProviderMock.get( inverseSideEntityTypeMock ) )
				.andStubReturn( inverseSideEntityTypeAdditionalMetadataMock );
		setupSingletonEmbeddedPropertiesAdditionalMetadataStub( inverseSideEntityTypeAdditionalMetadataMock, inverseSideProperty1Name );

		PojoTypeAdditionalMetadata inverseSideEmbeddableType1AdditionalMetadataMock =
				createMock( "inverseSideEmbeddableType1AdditionalMetadataMock", PojoTypeAdditionalMetadata.class );
		EasyMock.expect( typeAdditionalMetadataProviderMock.get( inverseSideEmbeddableType1Mock ) )
				.andStubReturn( inverseSideEmbeddableType1AdditionalMetadataMock );
		setupSingletonEmbeddedPropertiesAdditionalMetadataStub( inverseSideEmbeddableType1AdditionalMetadataMock, inverseSideProperty2Name );

		PojoTypeAdditionalMetadata inverseSideEmbeddableType2AdditionalMetadataMock =
				createMock( "inverseSideEmbeddableType2AdditionalMetadataMock", PojoTypeAdditionalMetadata.class );
		EasyMock.expect( typeAdditionalMetadataProviderMock.get( inverseSideEmbeddableType2Mock ) )
				.andStubReturn( inverseSideEmbeddableType2AdditionalMetadataMock );
		setupSingletonEmbeddedPropertiesAdditionalMetadataStub( inverseSideEmbeddableType2AdditionalMetadataMock, inverseSideProperty3Name );

		// Let's not complicate things any further: assume that none of the paths is the default one
		EasyMock.expect( extractorBinderMock.isDefaultExtractorPath(
				EasyMock.anyObject(), EasyMock.anyObject()
		) )
				.andStubReturn( false );
		// Let's not complicate things any further: assume that all extractor paths are noExtractors() paths
		EasyMock.expect( extractorBinderMock.bindPath(
				EasyMock.anyObject(),
				EasyMock.eq( ContainerExtractorPath.noExtractors() )
		) )
				.andStubAnswer( (IAnswer) () -> {
					PojoGenericTypeModel<?> sourceType =
							(PojoGenericTypeModel<?>) EasyMock.getCurrentArguments()[0];
					return BoundContainerExtractorPath.noExtractors( sourceType );
				} );

		replayAll();
		BoundPojoModelPathValueNode<?, ?, ?> boundPathToInvert =
				BoundPojoModelPathValueNode.root( originalSideEntityTypeMock )
						.property( originalSidePropertyName )
						.value( (BoundContainerExtractorPath) BoundContainerExtractorPath.noExtractors(
								originalSidePropertyTypeMock
						) );
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Found an infinite embedded recursion involving path '"
				+ PojoModelPath.builder()
						.property( inverseSideProperty1Name ).value( ContainerExtractorPath.noExtractors() )
						.property( inverseSideProperty2Name ).value( ContainerExtractorPath.noExtractors() )
						.property( inverseSideProperty3Name ).value( ContainerExtractorPath.noExtractors() )
						.toValuePath()
						.toPathString()
				+ "' on type '" + inverseSideEntityTypeMock.getName() + "'" );
		try {
			inverter.invertPath( inverseSideEntityTypeMock, boundPathToInvert );
		}
		finally {
			verifyAll();
		}
	}

	private void setupPropertyStub(PojoTypeModel<?> holdingTypeMock, String propertyName,
			PojoGenericTypeModel<?> propertyTypeMock) {
		ValueReadHandle<?> valueReadHandleMock =
				createMock( propertyName + "HandleMock", ValueReadHandle.class );
		PojoPropertyModel<?> propertyModelMock =
				createMock( propertyName + "ModelMock", PojoPropertyModel.class );
		EasyMock.expect( holdingTypeMock.getProperty( propertyName ) )
				.andStubReturn( (PojoPropertyModel) propertyModelMock );
		EasyMock.expect( propertyModelMock.getName() ).andStubReturn( propertyName );
		EasyMock.expect( propertyModelMock.getHandle() ).andStubReturn( (ValueReadHandle) valueReadHandleMock );
		EasyMock.expect( propertyModelMock.getTypeModel() )
				.andStubReturn( (PojoGenericTypeModel) propertyTypeMock );
	}

	private void setupSingletonEmbeddedPropertiesAdditionalMetadataStub(PojoTypeAdditionalMetadata typeAdditionalMetadata, String propertyName) {
		PojoPropertyAdditionalMetadata propertyAdditionalMetadataMock =
				createMock( propertyName + "PropertyAdditionalMetadataMock", PojoPropertyAdditionalMetadata.class );
		PojoValueAdditionalMetadata valueAdditionalMetadataMock =
				createMock( propertyName + "ValueAdditionalMetadataMock", PojoValueAdditionalMetadata.class );

		Map<String, PojoPropertyAdditionalMetadata> properties = new HashMap<>();
		properties.put( propertyName, propertyAdditionalMetadataMock );
		Map<ContainerExtractorPath, PojoValueAdditionalMetadata> values = new HashMap<>();
		values.put( ContainerExtractorPath.noExtractors(), valueAdditionalMetadataMock );

		EasyMock.expect( typeAdditionalMetadata.getPropertiesAdditionalMetadata() )
				.andStubReturn( properties );
		EasyMock.expect( propertyAdditionalMetadataMock.getValuesAdditionalMetadata() )
				.andStubReturn( values );
		EasyMock.expect( valueAdditionalMetadataMock.getInverseSidePath() )
				.andStubReturn( Optional.empty() );
		EasyMock.expect( valueAdditionalMetadataMock.isAssociationEmbedded() )
				.andStubReturn( true );
	}

}
