/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.building.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoPropertyAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoTypeAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoValueAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.SearchException;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@SuppressWarnings({"unchecked", "rawtypes"})
public class PojoAssociationPathInverterTest {

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	private PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProviderMock;
	@Mock
	private ContainerExtractorBinder extractorBinderMock;

	@Test
	public void detectInfiniteRecursion() {
		String originalSidePropertyName = "originalSideProperty";
		String inverseSideProperty1Name = "inverseSideProperty1";
		String inverseSideProperty2Name = "inverseSideProperty2";
		String inverseSideProperty3Name = "inverseSideProperty3";

		PojoAssociationPathInverter inverter =
				new PojoAssociationPathInverter( typeAdditionalMetadataProviderMock, extractorBinderMock );

		/*
		 * Original side model:
		 *
		 * - type
		 * -- property
		 */
		PojoRawTypeModel<?> originalSideEntityTypeMock =
				mock( PojoRawTypeModel.class, "originalSideEntityTypeMock" );
		when( originalSideEntityTypeMock.rawType() )
				.thenReturn( (PojoRawTypeModel) originalSideEntityTypeMock );

		PojoTypeModel<?> originalSidePropertyTypeMock =
				mock( PojoTypeModel.class, "originalSidePropertyTypeMock" );
		setupPropertyStub(
				originalSideEntityTypeMock, originalSidePropertyName, originalSidePropertyTypeMock
		);

		PojoTypeAdditionalMetadata originalSideEntityTypeAdditionalMetadataMock =
				mock( PojoTypeAdditionalMetadata.class, "originalSideEntityTypeAdditionalMetadataMock" );
		PojoPropertyAdditionalMetadata originalSidePropertyAdditionalMetadataMock =
				mock( PojoPropertyAdditionalMetadata.class, "originalSidePropertyAdditionalMetadataMock" );
		PojoValueAdditionalMetadata originalSideValueAdditionalMetadataMock =
				mock( PojoValueAdditionalMetadata.class, "originalSideValueAdditionalMetadataMock" );
		when( typeAdditionalMetadataProviderMock.get( originalSideEntityTypeMock ) )
				.thenReturn( originalSideEntityTypeAdditionalMetadataMock );
		when( originalSideEntityTypeAdditionalMetadataMock.getPropertyAdditionalMetadata( originalSidePropertyName ) )
				.thenReturn( originalSidePropertyAdditionalMetadataMock );
		when( originalSidePropertyAdditionalMetadataMock.getValueAdditionalMetadata( ContainerExtractorPath.noExtractors() ) )
				.thenReturn( originalSideValueAdditionalMetadataMock );
		when( originalSideValueAdditionalMetadataMock.getInverseSidePath() )
				.thenReturn( Optional.empty() );

		/*
		 * Inverse side model:
		 *
		 * - type
		 * -- property1 (embeddableType1)
		 * ---- property2 (embeddableType2)
		 * ------ property3 (embeddableType1) <- CYCLE
		 */
		PojoRawTypeModel<?> inverseSideEntityTypeMock =
				mock( PojoRawTypeModel.class, "inverseSideEntityTypeMock" );
		PojoRawTypeModel<?> inverseSideEmbeddableType1Mock =
				mock( PojoRawTypeModel.class, "inverseSideEmbeddableType1Mock" );
		PojoRawTypeModel<?> inverseSideEmbeddableType2Mock =
				mock( PojoRawTypeModel.class, "inverseSideEmbeddableType2Mock" );
		when( inverseSideEntityTypeMock.rawType() )
				.thenReturn( (PojoRawTypeModel) inverseSideEntityTypeMock );
		when( inverseSideEntityTypeMock.name() )
				.thenReturn( "inverseSideEntityTypeMock" );

		PojoTypeModel<?> inverseSideProperty1TypeMock =
				mock( PojoTypeModel.class, "inverseSideProperty1TypeMock" );
		setupPropertyStub(
				inverseSideEntityTypeMock, inverseSideProperty1Name, inverseSideProperty1TypeMock
		);
		when( inverseSideProperty1TypeMock.rawType() )
				.thenReturn( (PojoRawTypeModel) inverseSideEmbeddableType1Mock );

		PojoTypeModel<?> inverseSideProperty2TypeMock =
				mock( PojoTypeModel.class, "inverseSideProperty2TypeMock" );
		setupPropertyStub(
				inverseSideProperty1TypeMock, inverseSideProperty2Name, inverseSideProperty2TypeMock
		);
		when( inverseSideProperty2TypeMock.rawType() )
				.thenReturn( (PojoRawTypeModel) inverseSideEmbeddableType2Mock );

		setupPropertyStub(
				inverseSideProperty2TypeMock, inverseSideProperty3Name, inverseSideProperty2TypeMock
		);

		PojoTypeAdditionalMetadata inverseSideEntityTypeAdditionalMetadataMock =
				mock( PojoTypeAdditionalMetadata.class, "inverseSideEntityTypeAdditionalMetadataMock" );
		when( typeAdditionalMetadataProviderMock.get( inverseSideEntityTypeMock ) )
				.thenReturn( inverseSideEntityTypeAdditionalMetadataMock );
		setupSingletonEmbeddedPropertiesAdditionalMetadataStub( inverseSideEntityTypeAdditionalMetadataMock, inverseSideProperty1Name );

		PojoTypeAdditionalMetadata inverseSideEmbeddableType1AdditionalMetadataMock =
				mock( PojoTypeAdditionalMetadata.class, "inverseSideEmbeddableType1AdditionalMetadataMock" );
		when( typeAdditionalMetadataProviderMock.get( inverseSideEmbeddableType1Mock ) )
				.thenReturn( inverseSideEmbeddableType1AdditionalMetadataMock );
		setupSingletonEmbeddedPropertiesAdditionalMetadataStub( inverseSideEmbeddableType1AdditionalMetadataMock, inverseSideProperty2Name );

		PojoTypeAdditionalMetadata inverseSideEmbeddableType2AdditionalMetadataMock =
				mock( PojoTypeAdditionalMetadata.class, "inverseSideEmbeddableType2AdditionalMetadataMock" );
		when( typeAdditionalMetadataProviderMock.get( inverseSideEmbeddableType2Mock ) )
				.thenReturn( inverseSideEmbeddableType2AdditionalMetadataMock );
		setupSingletonEmbeddedPropertiesAdditionalMetadataStub( inverseSideEmbeddableType2AdditionalMetadataMock, inverseSideProperty3Name );

		// Let's not complicate things any further: assume that none of the paths is the default one
		when( extractorBinderMock.isDefaultExtractorPath( any(), any() ) )
				.thenReturn( false );
		// Let's not complicate things any further: assume that all extractor paths are noExtractors() paths
		when( extractorBinderMock.bindPath( any(), Mockito.eq( ContainerExtractorPath.noExtractors() ) ) )
				.thenAnswer( invocationOnMock -> {
					PojoTypeModel<?> sourceType = invocationOnMock.getArgument( 0 );
					return BoundContainerExtractorPath.noExtractors( sourceType );
				} );

		BoundPojoModelPathValueNode<?, ?, ?> boundPathToInvert =
				BoundPojoModelPathValueNode.root( originalSideEntityTypeMock )
						.property( originalSidePropertyName )
						.value( (BoundContainerExtractorPath) BoundContainerExtractorPath.noExtractors(
								originalSidePropertyTypeMock
						) );
		assertThatThrownBy( () -> inverter.invertPath( inverseSideEntityTypeMock, boundPathToInvert ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Infinite embedded recursion involving path '"
								+ PojoModelPath.builder()
										.property( inverseSideProperty1Name ).value( ContainerExtractorPath.noExtractors() )
										.property( inverseSideProperty2Name ).value( ContainerExtractorPath.noExtractors() )
										.property( inverseSideProperty3Name ).value( ContainerExtractorPath.noExtractors() )
										.toValuePath()
										.toPathString()
								+ "' on type '" + inverseSideEntityTypeMock.name() + "'"
				);
	}

	private void setupPropertyStub(PojoTypeModel<?> holdingTypeMock, String propertyName,
			PojoTypeModel<?> propertyTypeMock) {
		PojoPropertyModel<?> propertyModelMock = mock( PojoPropertyModel.class, propertyName + "ModelMock" );
		when( holdingTypeMock.property( propertyName ) )
				.thenReturn( (PojoPropertyModel) propertyModelMock );
		when( propertyModelMock.name() ).thenReturn( propertyName );
		when( propertyModelMock.typeModel() )
				.thenReturn( (PojoTypeModel) propertyTypeMock );
	}

	private void setupSingletonEmbeddedPropertiesAdditionalMetadataStub(PojoTypeAdditionalMetadata typeAdditionalMetadata, String propertyName) {
		PojoPropertyAdditionalMetadata propertyAdditionalMetadataMock =
				mock( PojoPropertyAdditionalMetadata.class, propertyName + "PropertyAdditionalMetadataMock" );
		PojoValueAdditionalMetadata valueAdditionalMetadataMock =
				mock( PojoValueAdditionalMetadata.class, propertyName + "ValueAdditionalMetadataMock" );

		Map<String, PojoPropertyAdditionalMetadata> properties = new HashMap<>();
		properties.put( propertyName, propertyAdditionalMetadataMock );
		Map<ContainerExtractorPath, PojoValueAdditionalMetadata> values = new HashMap<>();
		values.put( ContainerExtractorPath.noExtractors(), valueAdditionalMetadataMock );

		when( typeAdditionalMetadata.getNamesOfPropertiesWithAdditionalMetadata() )
				.thenReturn( properties.keySet() );
		when( typeAdditionalMetadata.getPropertyAdditionalMetadata( any() ) )
				.thenAnswer( invocation -> properties.get( (String) invocation.getArgument( 0 ) ) );
		when( propertyAdditionalMetadataMock.getValuesAdditionalMetadata() )
				.thenReturn( values );
		when( valueAdditionalMetadataMock.getInverseSidePath() )
				.thenReturn( Optional.empty() );
		when( valueAdditionalMetadataMock.isAssociationEmbedded() )
				.thenReturn( true );
	}

}
