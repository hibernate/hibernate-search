package org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;

import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import tools.jackson.databind.node.JsonNodeFactory;

class HttpAsyncContentProducerInputStreamTest {
	@Test
	void completedProducerSignalsEofToFreshStreamAndCloseAllowsReplay() throws Exception {
		var document = JsonNodeFactory.instance.objectNode().put( "value", "é😀" );
		var producer = new JsonHttpEntityContentProvider(
				org.hibernate.search.backend.elasticsearch.client.common.json.impl.ElasticsearchJsonBody
						.ndjson( List.of( document ), JsonProviderHelper.create( true ).getCodec().validation() ) );
		try ( var first = new HttpAsyncContentProducerInputStream( producer, 2 ) ) {
			byte[] expected = first.readAllBytes();
			assertThat( new String( expected, StandardCharsets.UTF_8 ) ).isEqualTo( "{\"value\":\"é😀\"}\n" );
			try ( var second = new HttpAsyncContentProducerInputStream( producer, 2 ) ) {
				assertThat( second.read( new byte[1] ) ).isEqualTo( -1 );
				assertThat( second.read() ).isEqualTo( -1 );
				assertThat( second.read( new byte[0] ) ).isZero();
			}
			try ( var replay = new HttpAsyncContentProducerInputStream( producer, 2 ) ) {
				for ( byte value : expected ) {
					assertThat( replay.read() ).isEqualTo( Byte.toUnsignedInt( value ) );
				}
				assertThat( replay.read() ).isEqualTo( -1 );
			}
		}
	}

	@Test
	void producerWithoutProgressFailsInsteadOfSpinningOrReturningPrematureEof() throws Exception {
		var producer = Mockito.mock( ContentProducer.class );
		try ( var stream = new HttpAsyncContentProducerInputStream( producer, 8 ) ) {
			assertThatThrownBy( () -> stream.read( new byte[1] ) ).isInstanceOf( IOException.class )
					.hasMessageContaining( "no progress" );
			assertThatThrownBy( stream::read ).isInstanceOf( IOException.class ).hasMessageContaining( "no progress" );
		}
		Mockito.verify( producer ).close();
	}

	@Test
	void bufferAndReadArgumentsAreValidated() throws Exception {
		var producer = Mockito.mock( ContentProducer.class );
		assertThatThrownBy( () -> new HttpAsyncContentProducerInputStream( producer, 0 ) )
				.isInstanceOf( IllegalArgumentException.class );
		try ( var stream = new HttpAsyncContentProducerInputStream( producer, 8 ) ) {
			assertThat( stream.read( new byte[0] ) ).isZero();
			assertThatThrownBy( () -> stream.read( new byte[1], 1, 1 ) ).isInstanceOf( IndexOutOfBoundsException.class );
			assertThatThrownBy( () -> stream.read( null, 0, 0 ) ).isInstanceOf( NullPointerException.class );
			Mockito.verifyNoInteractions( producer );
		}
	}
}
