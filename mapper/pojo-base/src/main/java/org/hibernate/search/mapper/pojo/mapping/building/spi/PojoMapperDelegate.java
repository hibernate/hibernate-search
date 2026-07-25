/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.building.spi;

import org.hibernate.search.engine.backend.mapping.spi.BackendMapperContext;
import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A delegate for the POJO mapper,
 * exposing hooks so that mappers based on the POJO mapper can consume metadata.
 *
 * @param <MPBS> The Java type of the partial build state of the produced mapping.
 */
public interface PojoMapperDelegate<MPBS> extends BackendMapperContext {

	/**
	 * Close any allocated resource.
	 * <p>
	 * This method is called when an error occurs while starting up Hibernate Search.
	 * When this method is called, it is guaranteed to be the last call on this object.
	 */
	void closeOnFailure();

	@Override
	default BackendMappingHints hints() {
		return BackendMappingHints.NONE;
	}

	/**
	 * @param rawTypeModel The raw type model for a type marked as an entity.
	 * @param entityName The name of the entity type.
	 * @return {@code true} if this delegate supports the given entity type, {@code false} if it should be ignored.
	 */
	@Incubating
	boolean isSupportedEntityType(PojoRawTypeModel<?> rawTypeModel, String entityName);

	/**
	 * @param <E> The indexed entity type.
	 * @param rawTypeModel The raw type model for an indexed entity type,
	 * i.e. a type mapped to an index directly.
	 * @param entityName The name of the entity type.
	 * @return A collector of extended mapping information.
	 */
	<E> PojoIndexedTypeExtendedMappingCollector createIndexedTypeExtendedMappingCollector(
			PojoRawTypeModel<E> rawTypeModel, String entityName);

	/**
	 * @param rawTypeModel The raw type model for a contained entity type,
	 * i.e. a type mapped to indexes only indirectly by indexed-embedding.
	 * @param entityName The name of the entity type.
	 * @param <E> The contained entity type.
	 * @return A collector of extended mapping information.
	 */
	<E> PojoContainedTypeExtendedMappingCollector createContainedTypeExtendedMappingCollector(
			PojoRawTypeModel<E> rawTypeModel, String entityName);

	/**
	 * Partially build the mapping based on the information provided previously.
	 * <p>
	 * May only be called once on a given object.
	 *
	 * @param mappingDelegate A {@link PojoMappingDelegate}.
	 * @return The partially-built mapping.
	 */
	MPBS prepareBuild(PojoMappingDelegate mappingDelegate);

}
