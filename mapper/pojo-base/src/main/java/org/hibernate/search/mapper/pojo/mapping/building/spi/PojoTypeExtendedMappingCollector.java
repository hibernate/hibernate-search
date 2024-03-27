/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import org.hibernate.search.engine.environment.bean.spi.ParameterizedBeanReference;
import org.hibernate.search.mapper.pojo.identity.spi.IdentifierMapping;
import org.hibernate.search.mapper.pojo.loading.definition.spi.PojoEntityLoadingBindingContext;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;

/**
 * A collector of extended mapping information.
 * <p>
 * This should be implemented by POJO mapper implementors in order to collect metadata
 * necessary to implement their {@link org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeExtendedContextProvider}.
 */
public interface PojoTypeExtendedMappingCollector {

	default void documentIdSourceProperty(PojoPropertyModel<?> documentIdSourceProperty) {
		// Default implementation: ignore this information.
	}

	default void identifierMapping(IdentifierMapping identifierMapping) {
		// Default implementation: ignore this information.
	}

	default void dirtyFilter(PojoPathFilter dirtyFilter) {
		// Default implementation: ignore this information.
	}

	default void dirtyContainingAssociationFilter(PojoPathFilter filter) {
		// Default implementation: ignore this information.
	}

	/**
	 * Apply a mapper-specific loading binder.
	 * <p>
	 * This is guaranteed to be called very late,
	 * and in particular after {@link #documentIdSourceProperty(PojoPropertyModel)}.
	 *
	 * @param binder The binder passed to
	 * {@link org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorEntityTypeNode#loadingBinder(ParameterizedBeanReference)}.
	 * @param context A context to get information and bind loading, e.g. set loading strategies.
	 */
	void applyLoadingBinder(Object binder, PojoEntityLoadingBindingContext context);

}
