/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.avro.impl;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.orm.coordination.databasepolling.avro.generated.impl.DirtinessDescriptorDto;
import org.hibernate.search.mapper.orm.coordination.databasepolling.avro.generated.impl.DocumentRouteDescriptorDto;
import org.hibernate.search.mapper.orm.coordination.databasepolling.avro.generated.impl.DocumentRoutesDescriptorDto;
import org.hibernate.search.mapper.orm.coordination.databasepolling.avro.generated.impl.PojoIndexingQueueEventPayloadDto;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.DirtinessDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;

public final class ModelConverterUtils {

	private ModelConverterUtils() {
	}

	static PojoIndexingQueueEventPayload convert(PojoIndexingQueueEventPayloadDto payload) {
		return new PojoIndexingQueueEventPayload( convert( payload.getRoutes() ), convert( payload.getDirtiness() ) );
	}

	static DirtinessDescriptor convert(DirtinessDescriptorDto dirtiness) {
		return new DirtinessDescriptor( dirtiness.getForceSelfDirty(), dirtiness.getForceContainingDirty(),
				convertDirtyPaths( dirtiness.getDirtyPaths() ), dirtiness.getUpdateBecauseOfContained()
		);
	}

	private static Set<String> convertDirtyPaths(List<CharSequence> dirtyPaths) {
		return dirtyPaths.stream().map( CharSequence::toString ).collect( Collectors.toSet() );
	}

	static DocumentRoutesDescriptor convert(DocumentRoutesDescriptorDto routes) {
		return new DocumentRoutesDescriptor(
				convert( routes.getCurrentRoute() ), convertRoutes( routes.getPreviousRoutes() ) );
	}

	private static Collection<DocumentRouteDescriptor> convertRoutes(List<DocumentRouteDescriptorDto> routes) {
		return routes.stream().map( route -> convert( route ) )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
	}

	static DocumentRouteDescriptor convert(DocumentRouteDescriptorDto route) {
		CharSequence routingKey = route.getRoutingKey();
		return DocumentRouteDescriptor.of( ( routingKey == null ) ? null : routingKey.toString() );
	}
}
