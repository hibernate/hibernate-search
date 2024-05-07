/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.reporting.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.reporting.spi.MapperHints;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface HibernateOrmMapperHints extends MapperHints {

	HibernateOrmMapperHints INSTANCE = Messages.getBundle( MethodHandles.lookup(), HibernateOrmMapperHints.class );

	@Message("Prebuild the missing Jandex indexes and make them available to Hibernate Search"
			+ " or use a combination of configuration properties ("
			+ " \"" + HibernateOrmMapperSettings.MAPPING_CONFIGURER + "\","
			+ " \"" + HibernateOrmMapperSettings.MAPPING_DISCOVER_ANNOTATED_TYPES_FROM_ROOT_MAPPING_ANNOTATIONS + "\","
			+ " \"" + HibernateOrmMapperSettings.MAPPING_BUILD_MISSING_DISCOVERED_JANDEX_INDEXES + "\""
			+ ") to limit the discovery of annotated types."
			+ " For specifics, refer to the \"Classpath scanning\""
			+ " section of the reference documentation.")
	String cannotReadJandexRootMapping();

}
