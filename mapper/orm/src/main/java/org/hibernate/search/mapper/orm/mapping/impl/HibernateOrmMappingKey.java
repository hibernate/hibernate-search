/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.mapper.orm.reporting.impl.HibernateOrmEventContextMessages;

public final class HibernateOrmMappingKey
		implements MappingKey<HibernateOrmMappingPartialBuildState, HibernateOrmMapping> {

	private static final HibernateOrmEventContextMessages MESSAGES = HibernateOrmEventContextMessages.INSTANCE;

	@Override
	public String render() {
		return MESSAGES.mapping();
	}

}
