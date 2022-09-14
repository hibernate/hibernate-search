/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.reporting.impl;

import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface StandalonePojoMappingHints extends BackendMappingHints {

	StandalonePojoMappingHints INSTANCE = Messages.getBundle( StandalonePojoMappingHints.class );

	@Message("To enable loading of entity instances from an external source, provide a SelectionLoadingStrategy"
			+ " when registering the entity type to the mapping builder."
			+ " To enable projections turning taking index data into entity instances,"
			+ " annotate a constructor of the entity type with @ProjectionConstructor."
			+ "See the reference documentation for more information.")
	@Override
	String noEntityProjectionAvailable();

}
