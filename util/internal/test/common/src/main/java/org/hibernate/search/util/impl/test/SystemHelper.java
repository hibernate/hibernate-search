/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.hibernate.search.util.impl.test.annotation.SuppressForbiddenApis;

import org.awaitility.Awaitility;

public final class SystemHelper {

	private SystemHelper() {
	}

	@SuppressForbiddenApis(reason = "This is a safer wrapper around System.setProperty")
	public static SystemPropertyRestorer setSystemProperty(String key, String value) {
		String oldValue = System.getProperty( key );
		System.setProperty( key, value );
		return oldValue == null
				? () -> System.clearProperty( key )
				: () -> System.setProperty( key, oldValue );
	}

	public static Process runCommandWithInheritedIO(String... command) throws IOException {
		// We can't use `inheritIO()` in surefire as that would bypass surefire's
		// output capturing and lead to warnings.
		// So we use an old-fashioned solution...

		Process process = new ProcessBuilder( command )
				.redirectInput( ProcessBuilder.Redirect.INHERIT )
				.start();

		// Drain the output/errors streams
		ExecutorService service = Executors.newFixedThreadPool( 2 );
		try ( InputStream is = process.getInputStream(); InputStream es = process.getErrorStream();
				Closeable pool = service::shutdownNow ) {
			service.submit( () -> drain( is, System.out::println ) );
			service.submit( () -> drain( es, System.err::println ) );
			service.shutdown();

			Awaitility.await( process + " termination" )
					.atMost( 1, TimeUnit.MINUTES )
					.until( () -> !process.isAlive() );
		}

		return process;
	}

	private static void drain(InputStream stream, Consumer<String> consumer) {
		try ( InputStreamReader in = new InputStreamReader( stream, StandardCharsets.UTF_8 );
				BufferedReader bufferedReader = new BufferedReader( in ); ) {
			bufferedReader.lines().forEach( consumer );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	public interface SystemPropertyRestorer extends AutoCloseable {
		@Override
		void close();
	}
}
