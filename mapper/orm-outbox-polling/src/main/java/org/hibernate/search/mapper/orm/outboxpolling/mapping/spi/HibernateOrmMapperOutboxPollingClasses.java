/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.mapping.spi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class HibernateOrmMapperOutboxPollingClasses {

	private HibernateOrmMapperOutboxPollingClasses() {
	}

	/**
	 * @return A set of names of all classes that will be involved in Avro serialization
	 * and thus will require reflection support.
	 * Useful to enable reflection for these classes in GraalVM-based native images.
	 */
	public static Set<String> avroTypes() {
		return new HashSet<>( Arrays.asList(
				"org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.DocumentRoutesDescriptorDto$Builder",
				"org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.DirtinessDescriptorDto$Builder",
				"org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.DirtinessDescriptorDto",
				"org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.DocumentRoutesDescriptorDto",
				"org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.DocumentRouteDescriptorDto$Builder",
				"org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.PojoIndexingQueueEventPayloadDto",
				"org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.DocumentRouteDescriptorDto",
				"org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.PojoIndexingQueueEventPayloadDto$Builder"
		) );
	}

	/**
	 * @return A set of names of all classes that will be involved in Hibernate ORM mapping
	 * and thus will require reflection support.
	 * Useful to enable reflection for these classes in GraalVM-based native images.
	 */
	public static Set<String> hibernateOrmTypes() {
		return new HashSet<>( Arrays.asList(
				"org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentType",
				"org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEvent",
				"org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent",
				"org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEvent$Status",
				"org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentState"
		) );
	}

}
