/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.avro.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.DirtinessDescriptorDto;
import org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.DocumentRouteDescriptorDto;
import org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.DocumentRoutesDescriptorDto;
import org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.PojoIndexingQueueEventPayloadDto;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.DirtinessDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;

final class EventPayloadToDtoConverterUtils {

	private EventPayloadToDtoConverterUtils() {
	}

	static PojoIndexingQueueEventPayloadDto convert(PojoIndexingQueueEventPayload payload) {
		return PojoIndexingQueueEventPayloadDto.newBuilder()
				.setDirtiness( convert( payload.dirtiness ) )
				.setRoutes( convert( payload.routes ) )
				.build();
	}

	private static DirtinessDescriptorDto convert(DirtinessDescriptor dirtiness) {
		return DirtinessDescriptorDto.newBuilder()
				.setDirtyPaths( convertDirtyPaths( dirtiness.dirtyPaths() ) )
				.setForceContainingDirty( dirtiness.forceContainingDirty() )
				.setForceSelfDirty( dirtiness.forceSelfDirty() )
				.setUpdateBecauseOfContained( dirtiness.updatedBecauseOfContained() )
				.build();
	}

	private static List<CharSequence> convertDirtyPaths(Set<String> dirtyPaths) {
		return new ArrayList<>( dirtyPaths );
	}

	private static DocumentRoutesDescriptorDto convert(DocumentRoutesDescriptor routes) {
		return DocumentRoutesDescriptorDto.newBuilder()
				.setCurrentRoute( convert( routes.currentRoute() ) )
				.setPreviousRoutes( convertRoutes( routes.previousRoutes() ) )
				.build();
	}

	private static List<DocumentRouteDescriptorDto> convertRoutes(Collection<DocumentRouteDescriptor> routes) {
		return routes.stream().map( EventPayloadToDtoConverterUtils::convert ).collect( Collectors.toList() );
	}

	private static DocumentRouteDescriptorDto convert(DocumentRouteDescriptor route) {
		if ( route == null ) {
			return null;
		}
		return DocumentRouteDescriptorDto.newBuilder()
				.setRoutingKey( route.routingKey() )
				.build();
	}
}
