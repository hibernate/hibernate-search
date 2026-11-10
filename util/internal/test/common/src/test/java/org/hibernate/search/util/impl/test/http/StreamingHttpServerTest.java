package org.hibernate.search.util.impl.test.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StreamingHttpServerTest {
	@ParameterizedTest
	@ValueSource(strings = {
			"POST / HTTP/1.1\r\n",
			"POST / HTTP/1.1\r\nContent-Ty",
			"POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n",
			"POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\na",
			"POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n2\r\nx",
			"POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n2\r\nxx\r\n",
			"POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n0\r\n"
	})
	void disconnectDuringRequestIsAnExpectedOutcome(String request) throws Exception {
		try ( var server = new StreamingHttpServer( exchange -> exchange.readBody( (bytes, count) -> {} ) ) ) {
			URI uri = URI.create( server.uri() );
			try ( var socket = new Socket( uri.getHost(), uri.getPort() ) ) {
				socket.setSoTimeout( 10_000 );
				socket.getOutputStream().write( request.getBytes( StandardCharsets.US_ASCII ) );
				socket.shutdownOutput();
				assertThat( socket.getInputStream().read() ).isEqualTo( -1 );
			}
		}
	}
}
