/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.reporting.impl;

import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface HibernateOrmMappingHints extends BackendMappingHints {

	HibernateOrmMappingHints INSTANCE = Messages.getBundle( HibernateOrmMappingHints.class );

	@Override
	@Message(value = "This is likely a bug, as Hibernate ORM entities should always be loadable from the database.")
	String noEntityProjectionAvailable();

	@Override
	@Message("If you used a @*Field annotation here, make sure to use @ScaledNumberField and configure the `decimalScale` attribute as necessary.")
	String missingDecimalScale();

	@Override
	@Message("Either specify dimension as an annotation property (@VectorField(dimension = somePositiveInteger)), "
			+ "or define a value binder (@VectorField(valueBinder = @ValueBinderRef(..))) that explicitly declares a vector field specifying the dimension.")
	String missingVectorDimension();
}
