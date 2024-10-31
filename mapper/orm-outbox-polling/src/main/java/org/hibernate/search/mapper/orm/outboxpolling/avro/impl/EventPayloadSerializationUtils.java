/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.avro.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.PojoIndexingQueueEventPayloadDto;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.OutboxPollingEventsLog;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

public final class EventPayloadSerializationUtils {

	private EventPayloadSerializationUtils() {
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
				PojoIndexingQueueEventPayloadDto.class );

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
