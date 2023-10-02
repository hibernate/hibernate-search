/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.avro.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.DirtinessDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;

import org.junit.Ignore;
import org.junit.Test;

import org.apache.avro.Protocol;

public class EventPayloadSerializationUtilsTest {

	private static final Set<String> DIRTY_PATHS = Set.of( "a", "b", "c" );
	private static final String ROUTING_KEY = "key1";

	@Test
	@Ignore("Enable this test, or just run it from an IDE once you need to create a new payload for a newer version of Avro.")
	public void createNewAvroPayloadFile() throws IOException, URISyntaxException {
		PojoIndexingQueueEventPayload payload = new PojoIndexingQueueEventPayload(
				new DocumentRoutesDescriptor(
						DocumentRouteDescriptor.of( ROUTING_KEY ),
						new ArrayList<>(
								List.of( DocumentRouteDescriptor.of( "key2" ), DocumentRouteDescriptor.of( "key3" ) ) )
				),
				new DirtinessDescriptor(
						true,
						true,
						DIRTY_PATHS,
						false
				)
		);

		byte[] serialized = EventPayloadSerializationUtils.serialize( payload );

		Path testDataDirectory = payloadTestResourceLocation();

		Files.createDirectories( testDataDirectory );
		try ( OutputStream out = Files.newOutputStream(
				testDataDirectory.resolve( "payload-avro-" + Protocol.class.getPackage().getImplementationVersion() ) ) ) {
			out.write( serialized );
		}
	}

	@Test
	public void canDeserializePreviousPayloads() throws IOException, URISyntaxException {
		try ( Stream<Path> payloads = Files.list( payloadTestResourceLocation() ) ) {
			payloads.forEach( payload -> {
				try ( InputStream inputStream = Files.newInputStream( payload ) ) {
					PojoIndexingQueueEventPayload deserialized =
							EventPayloadSerializationUtils.deserialize( inputStream.readAllBytes() );
					assertThat( deserialized )
							.as( payload.toString() )
							.isNotNull();
					assertThat( deserialized.routes.currentRoute().routingKey() )
							.as( payload.toString() )
							.isEqualTo( ROUTING_KEY );
					assertThat( deserialized.dirtiness.dirtyPaths() )
							.as( payload.toString() )
							.containsAll( DIRTY_PATHS );
				}
				catch (IOException e) {
					fail( "Cannot read payload from " + payload + ". Reason: " + e );
				}

			} );
		}
	}

	private static Path payloadTestResourceLocation() throws URISyntaxException {
		return Path.of(
				EventPayloadSerializationUtilsTest.class.getProtectionDomain().getCodeSource().getLocation().toURI() )
				.getParent().getParent().resolve( "src/test/resources/avro" );
	}
}
