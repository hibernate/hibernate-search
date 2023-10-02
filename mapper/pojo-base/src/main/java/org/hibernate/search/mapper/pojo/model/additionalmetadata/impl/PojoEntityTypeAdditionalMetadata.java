/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.impl;

import java.util.Optional;

import org.hibernate.search.engine.environment.bean.spi.ParameterizedBeanReference;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathDefinitionProvider;

public class PojoEntityTypeAdditionalMetadata {
	private final String entityName;
	private final String secondaryEntityName;
	private final PojoPathDefinitionProvider pathDefinitionProvider;
	private final Optional<String> entityIdPropertyName;
	private final ParameterizedBeanReference<?> loadingBinderRef;

	public PojoEntityTypeAdditionalMetadata(String entityName, String secondaryEntityName,
			PojoPathDefinitionProvider pathDefinitionProvider,
			Optional<String> entityIdPropertyName,
			ParameterizedBeanReference<?> loadingBinderRef) {
		this.entityName = entityName;
		this.secondaryEntityName = secondaryEntityName;
		this.pathDefinitionProvider = pathDefinitionProvider;
		this.entityIdPropertyName = entityIdPropertyName;
		this.loadingBinderRef = loadingBinderRef;
	}

	public String getEntityName() {
		return entityName;
	}

	public String getSecondaryEntityName() {
		return secondaryEntityName;
	}

	public PojoPathDefinitionProvider pathDefinitionProvider() {
		return pathDefinitionProvider;
	}

	public Optional<String> getEntityIdPropertyName() {
		return entityIdPropertyName;
	}

	public ParameterizedBeanReference<?> getLoadingBinderRef() {
		return loadingBinderRef;
	}
}
