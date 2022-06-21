/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.mapping.impl;

import org.hibernate.search.mapper.pojo.standalone.log.impl.StandalonePojoEventContextMessages;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;

public final class StandalonePojoMappingKey implements MappingKey<StandalonePojoMappingPartialBuildState, StandalonePojoMapping> {
	private static final StandalonePojoEventContextMessages MESSAGES = StandalonePojoEventContextMessages.INSTANCE;

	@Override
	public String render() {
		return MESSAGES.mapping();
	}
}
