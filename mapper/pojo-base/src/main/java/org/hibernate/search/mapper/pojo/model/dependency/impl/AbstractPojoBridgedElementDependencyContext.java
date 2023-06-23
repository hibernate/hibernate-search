/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.dependency.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathOriginalTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoModelPathBinder;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractPojoBridgedElementDependencyContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoBootstrapIntrospector introspector;
	final BoundPojoModelPath.Walker bindingPathWalker;
	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;

	private boolean useRootOnly;

	AbstractPojoBridgedElementDependencyContext(
			PojoBootstrapIntrospector introspector,
			ContainerExtractorBinder containerExtractorBinder,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider) {
		this.introspector = introspector;
		this.bindingPathWalker = BoundPojoModelPath.walker( containerExtractorBinder );
		this.typeAdditionalMetadataProvider = typeAdditionalMetadataProvider;
	}

	public void useRootOnly() {
		this.useRootOnly = true;
	}

	public boolean isUseRootOnly() {
		return useRootOnly;
	}

	public abstract boolean hasNonRootDependency();

	PojoOtherEntityIndexingDependencyConfigurationContextImpl<?> createOtherEntityDependencyContext(
			PojoRawTypeModel<?> bridgedType,
			Class<?> otherEntityClass, PojoModelPathValueNode pathFromOtherEntityTypeToBridgedType) {
		if ( !typeAdditionalMetadataProvider.get( bridgedType ).isEntity() ) {
			throw log.cannotDefineOtherEntityDependencyOnNonEntityBridgedType( bridgedType );
		}

		PojoRawTypeModel<?> otherEntityType = introspector.typeModel( otherEntityClass );
		if ( !typeAdditionalMetadataProvider.get( otherEntityType ).isEntity() ) {
			throw log.cannotDefineOtherEntityDependencyFromNonEntityType( otherEntityType );
		}

		BoundPojoModelPathOriginalTypeNode<?> otherEntityRootPath = BoundPojoModelPath.root( otherEntityType );

		BoundPojoModelPathValueNode<?, ?, ?> boundPathFromOtherEntityTypeToBridgedType =
				PojoModelPathBinder.bind(
						otherEntityRootPath,
						pathFromOtherEntityTypeToBridgedType,
						bindingPathWalker
				);

		PojoRawTypeModel<?> inverseSideRawType = boundPathFromOtherEntityTypeToBridgedType.getTypeModel().rawType();
		if ( !inverseSideRawType.isSubTypeOf( bridgedType ) && !bridgedType.isSubTypeOf( inverseSideRawType ) ) {
			throw log.incorrectTargetTypeForInverseAssociation( inverseSideRawType, bridgedType );
		}

		return new PojoOtherEntityIndexingDependencyConfigurationContextImpl<>(
				bindingPathWalker, otherEntityRootPath, boundPathFromOtherEntityTypeToBridgedType
		);
	}

}
