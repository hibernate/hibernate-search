/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.avro.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.DirtinessDescriptorDto;
import org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.DocumentRouteDescriptorDto;
import org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.DocumentRoutesDescriptorDto;
import org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.PojoIndexingQueueEventPayloadDto;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.OutboxPollingEventsLog;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

public final class EventPayloadSerializationUtils {

	/**
	 * A dedicated {@link SpecificData} whose only job is to resolve <em>our</em> generated DTO classes
	 * when deserializing, without ever consulting Avro's class-security validator.
	 * <p>
	 * Avro 1.12.2 introduced {@code org.apache.avro.util.ClassSecurityValidator}: during deserialization,
	 * every class Avro has to instantiate from a schema name is checked against a trust list, and by default
	 * only JDK types are trusted. Since our payloads reference our own generated DTOs, plain deserialization
	 * now fails with a {@code SecurityException}.
	 * <p>
	 * We deliberately keep the fix local to this (de)serialization path: by resolving our DTO classes directly
	 * here, Avro never looks them up by name, so the validator is simply never consulted for our payloads.
	 * <p>
	 * We intentionally did <strong>not</strong> use the alternative global approach:
	 * <pre>{@code
	 * // Registers our classes on Avro's single, JVM-wide validator:
	 * ClassSecurityValidator.setGlobal(
	 *         ClassSecurityValidator.composite(
	 *                 ClassSecurityValidator.getGlobal(),
	 *                 ClassSecurityValidator.builder()
	 *                         .add( PojoIndexingQueueEventPayloadDto.class )
	 *                         .add( DocumentRoutesDescriptorDto.class )
	 *                         .add( DocumentRouteDescriptorDto.class )
	 *                         .add( DirtinessDescriptorDto.class )
	 *                         .build() ) );
	 * }</pre>
	 * because {@code globalInstance} is a single, mutable, JVM-wide field: our registration would be visible to
	 * (and could be silently overwritten by) any other code calling {@code setGlobal(...)}, making correctness
	 * depend on class-initialization order. The local approach below has no such shared state.
	 */
	private static final SpecificData SPECIFIC_DATA = createSpecificData();

	private EventPayloadSerializationUtils() {
	}

	private static SpecificData createSpecificData() {
		// The Avro "full name" of a generated record equals the Java class name (its namespace is the package),
		// so we can key our known classes directly by Schema#getFullName().
		Map<String, Class<?>> knownClasses = Map.of(
				PojoIndexingQueueEventPayloadDto.class.getName(), PojoIndexingQueueEventPayloadDto.class,
				DocumentRoutesDescriptorDto.class.getName(), DocumentRoutesDescriptorDto.class,
				DocumentRouteDescriptorDto.class.getName(), DocumentRouteDescriptorDto.class,
				DirtinessDescriptorDto.class.getName(), DirtinessDescriptorDto.class
		);
		return new SpecificData() {
			@Override
			public Class<?> getClass(Schema schema) {
				Class<?> known = knownClasses.get( schema.getFullName() );
				return known != null ? known : super.getClass( schema );
			}
		};
	}

	public static byte[] serialize(PojoIndexingQueueEventPayload payload) {
		SpecificDatumWriter<PojoIndexingQueueEventPayloadDto> writer =
				new SpecificDatumWriter<>( PojoIndexingQueueEventPayloadDto.class );

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Encoder encoder = EncoderFactory.get().directBinaryEncoder( out, null );

		try {
			writer.write( EventPayloadToDtoConverterUtils.convert( payload ), encoder );
			encoder.flush();
		}
		catch (IOException | RuntimeException e) {
			throw OutboxPollingEventsLog.INSTANCE.unableToSerializeOutboxEventPayloadWithAvro( e.getMessage(), e );
		}

		return out.toByteArray();
	}

	public static PojoIndexingQueueEventPayload deserialize(byte[] payloadData) {
		SpecificDatumReader<PojoIndexingQueueEventPayloadDto> reader = new SpecificDatumReader<>(
				PojoIndexingQueueEventPayloadDto.getClassSchema(), PojoIndexingQueueEventPayloadDto.getClassSchema(),
				SPECIFIC_DATA );

		ByteArrayInputStream in = new ByteArrayInputStream( payloadData );
		BinaryDecoder decoder = DecoderFactory.get().binaryDecoder( in, null );

		try {
			return EventPayloadFromDtoConverterUtils.convert( reader.read( null, decoder ) );
		}
		catch (IOException | RuntimeException e) {
			throw OutboxPollingEventsLog.INSTANCE.unableToDeserializeOutboxEventPayloadWithAvro( e.getMessage(), e );
		}
	}
}
