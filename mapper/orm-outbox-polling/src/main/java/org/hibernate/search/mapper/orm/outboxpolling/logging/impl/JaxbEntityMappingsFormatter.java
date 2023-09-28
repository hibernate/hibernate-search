/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.logging.impl;

import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.search.mapper.orm.outboxpolling.mapping.impl.JaxbMappingHelper;

/**
 * Used with JBoss Logging's {@link org.jboss.logging.annotations.FormatWith} to format
 * {@link JaxbEntityMappings} objects using marshaling.
 */
public final class JaxbEntityMappingsFormatter {

	private final JaxbEntityMappings mappings;

	public JaxbEntityMappingsFormatter(JaxbEntityMappings mappings) {
		this.mappings = mappings;
	}

	@Override
	public String toString() {
		return JaxbMappingHelper.marshall( mappings );
	}
}
