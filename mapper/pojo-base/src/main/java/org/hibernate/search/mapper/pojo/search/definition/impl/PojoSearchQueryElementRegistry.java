/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.search.projection.definition.spi.CompositeProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.spi.ProjectionRegistry;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoSearchQueryElementRegistry implements ProjectionRegistry {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public <T> CompositeProjectionDefinition<T> composite(Class<T> objectClass) {
		// TODO HSEARCH-3927 collect projection definitions on startup
		throw log.invalidObjectClassForProjection( objectClass );
	}
}
