/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.impl;

import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.hibernate.search.metamodel.processor.HibernateSearchMetamodelProcessorSettings.Configuration;

public record HibernateSearchMetamodelProcessorContext( Elements elementUtils, Types typeUtils, Messager messager,
														javax.annotation.processing.Filer filer, Configuration configuration) {

	public boolean isOrmMapperPresent() {
		return configuration.isOrmMapperPresent( elementUtils() );
	}

}
