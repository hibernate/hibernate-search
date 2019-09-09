/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.log.impl.JavaBeanEventContextMessages;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;

import org.jboss.logging.Messages;

public final class JavaBeanMappingKey implements MappingKey<JavaBeanMappingPartialBuildState, SearchMapping> {
	private static final JavaBeanEventContextMessages MESSAGES =
			Messages.getBundle( JavaBeanEventContextMessages.class );

	@Override
	public String render() {
		return MESSAGES.mapping();
	}
}
