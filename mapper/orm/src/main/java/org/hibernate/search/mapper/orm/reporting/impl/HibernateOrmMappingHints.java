/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
}
