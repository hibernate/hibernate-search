package org.hibernate.search.util.impl.test.http;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Local HTTP/1.1 fixture with explicit transfer gates, bounded storage, and connection identities. */
public final class StreamingHttpServer implements AutoCloseable {
	@FunctionalInterface
	public interface Handler {
		void handle(Exchange exchange) throws Exception;
	}

	@FunctionalInterface
	public interface BodyConsumer {
		void accept(byte[] bytes, int length) throws Exception;
	}

	private final ServerSocket listener;
	private final ExecutorService workers = Executors.newCachedThreadPool();
	private final List<Socket> sockets = new ArrayList<>();
	private final AtomicInteger connectionSequence = new AtomicInteger();
	private final CompletableFuture<Void> failure = new CompletableFuture<>();
	private volatile boolean closed;

	public StreamingHttpServer(Handler handler) throws IOException {
		listener = new ServerSocket( 0, 50, InetAddress.getLoopbackAddress() );
		workers.submit( () -> {
			while ( !closed ) {
				try {
					Socket socket = listener.accept();
					socket.setSoTimeout( 15_000 );
					synchronized (sockets) {
						sockets.add( socket );
					}
					int connection = connectionSequence.incrementAndGet();
					workers.submit( () -> serve( socket, connection, handler ) );
				}
				catch (IOException e) {
					if ( !closed ) {
						failure.completeExceptionally( e );
					}
				}
			}
		} );
	}

	public String uri() {
		return "http://localhost:" + listener.getLocalPort();
	}

	public void checkFailure() {
		if ( failure.isCompletedExceptionally() ) {
			failure.join();
		}
	}

	private void serve(Socket socket, int connection, Handler handler) {
		try ( socket ) {
			InputStream input = new BufferedInputStream( socket.getInputStream(), 8192 );
			while ( !closed ) {
				String requestLine = line( input );
				if ( requestLine == null ) {
					return;
				}
				Map<String, String> headers = new HashMap<>();
				String header;
				while ( !( header = requiredLine( input ) ).isEmpty() ) {
					int colon = header.indexOf( ':' );
					headers.put( header.substring( 0, colon ).toLowerCase( Locale.ROOT ),
							header.substring( colon + 1 ).trim() );
				}
				Exchange exchange = new Exchange( connection, requestLine, headers, input, socket.getOutputStream() );
				handler.handle( exchange );
				if ( exchange.closeConnection ) {
					return;
				}
				exchange.readBody( (bytes, count) -> {} );
			}
		}
		catch (IOException e) {
			// Client cancellation, early rejection and connection closure are expected test outcomes.
		}
		catch (Exception e) {
			if ( !closed ) {
				failure.completeExceptionally( e );
			}
		}
	}

	private static String line(InputStream input) throws IOException {
		StringBuilder result = new StringBuilder();
		int value;
		while ( ( value = input.read() ) != -1 ) {
			if ( value == '\n' ) {
				return result.toString();
			}
			if ( value != '\r' ) {
				result.append( (char) value );
			}
			if ( result.length() > 16_384 ) {
				throw new IOException( "HTTP fixture header too long" );
			}
		}
		if ( result.length() != 0 ) {
			throw new EOFException( "Incomplete HTTP line" );
		}
		return null;
	}

	private static String requiredLine(InputStream input) throws IOException {
		String result = line( input );
		if ( result == null ) {
			throw new EOFException();
		}
		return result;
	}

	private static long chunkSize(InputStream input) throws IOException {
		try {
			long size = Long.parseLong( requiredLine( input ).split( ";", 2 )[0], 16 );
			if ( size < 0 ) {
				throw new IOException( "Negative HTTP chunk size" );
			}
			return size;
		}
		catch (NumberFormatException e) {
			throw new IOException( "Invalid or incomplete HTTP chunk size", e );
		}
	}

	@Override
	public void close() throws IOException {
		closed = true;
		listener.close();
		synchronized (sockets) {
			for ( Socket socket : sockets ) {
				socket.close();
			}
		}
		workers.shutdownNow();
		try {
			if ( !workers.awaitTermination( 10, TimeUnit.SECONDS ) ) {
				throw new IOException( "HTTP fixture workers did not terminate" );
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException( e );
		}
		checkFailure();
	}

	public static final class Exchange {
		public final int connection;
		public final String requestLine;
		public final Map<String, String> headers;
		private final InputStream input;
		private final OutputStream output;
		private boolean bodyRead;
		private boolean closeConnection;

		private Exchange(int connection, String requestLine, Map<String, String> headers, InputStream input,
				OutputStream output) {
			this.connection = connection;
			this.requestLine = requestLine;
			this.headers = headers;
			this.input = input;
			this.output = output;
		}

		public void readBody(BodyConsumer consumer) throws Exception {
			if ( bodyRead ) {
				return;
			}
			bodyRead = true;
			byte[] buffer = new byte[8192];
			boolean chunked = "chunked".equalsIgnoreCase( headers.get( "transfer-encoding" ) );
			long count = chunked
					? chunkSize( input )
					: Long.parseLong( headers.getOrDefault( "content-length", "0" ) );
			while ( count > 0 ) {
				while ( count > 0 ) {
					int read = input.read( buffer, 0, (int) Math.min( count, buffer.length ) );
					if ( read < 0 ) {
						throw new EOFException();
					}
					consumer.accept( buffer, read );
					count -= read;
				}
				if ( !chunked ) {
					return;
				}
				requiredLine( input );
				count = chunkSize( input );
			}
			if ( chunked ) {
				while ( !requiredLine( input ).isEmpty() ) {
					// Drain trailers.
				}
			}
		}

		public void headers(int status, String contentType, String extraHeaders) throws IOException {
			output.write( ( "HTTP/1.1 " + status + " fixture\r\nTransfer-Encoding: chunked\r\nContent-Type: "
					+ contentType + "\r\n" + extraHeaders + "\r\n" ).getBytes( StandardCharsets.ISO_8859_1 ) );
			output.flush();
		}

		public void chunk(byte[] bytes) throws IOException {
			output.write( ( Integer.toHexString( bytes.length ) + "\r\n" ).getBytes( StandardCharsets.US_ASCII ) );
			output.write( bytes );
			output.write( "\r\n".getBytes( StandardCharsets.US_ASCII ) );
			output.flush();
		}

		public void end() throws IOException {
			output.write( "0\r\n\r\n".getBytes( StandardCharsets.US_ASCII ) );
			output.flush();
		}

		public void closeConnection() {
			closeConnection = true;
		}

		/** Wait for the client to close its connection; the socket has a finite read timeout. */
		public boolean awaitDisconnect() throws IOException {
			return input.read() == -1;
		}
	}
}
