/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.avro.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.orm.outboxpolling.avro.generated.impl.PojoIndexingQueueEventPayloadDto;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

public final class EventPayloadSerializationUtils {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
			throw log.unableToSerializeOutboxEventPayloadWithAvro( e.getMessage(), e );
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
			throw log.unableToDeserializeOutboxEventPayloadWithAvro( e.getMessage(), e );
		}
	}
}
