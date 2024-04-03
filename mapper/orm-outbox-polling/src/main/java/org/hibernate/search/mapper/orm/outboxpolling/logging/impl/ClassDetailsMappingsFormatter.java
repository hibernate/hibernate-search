/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.logging.impl;

import java.util.Objects;

import org.hibernate.models.spi.ClassDetails;

/**
 * Used with JBoss Logging's {@link org.jboss.logging.annotations.FormatWith} to format
 * {@link ClassDetails} objects using marshaling.
 */
public final class ClassDetailsMappingsFormatter {

	private final ClassDetails mappings;

	public ClassDetailsMappingsFormatter(ClassDetails mappings) {
		this.mappings = mappings;
	}

	@Override
	public String toString() {
		// TODO: need better to-string impl:
		return Objects.toString( mappings );
	}
}
