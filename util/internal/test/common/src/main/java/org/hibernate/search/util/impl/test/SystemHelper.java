/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.util.impl.test.annotation.SuppressForbiddenApis;

import com.google.common.base.Charsets;

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
		// output capturingand lead to warnings.
		// So we use an old-fashioned solution...

		Process process = new ProcessBuilder( command )
				.redirectInput( ProcessBuilder.Redirect.INHERIT )
				.start();

		// Drain the output/errors streams
		ExecutorService service = Executors.newFixedThreadPool( 2 );
		service.submit( () -> new BufferedReader( new InputStreamReader( process.getInputStream(), Charsets.UTF_8 ) )
				.lines()
				.forEach( System.out::println ) );
		service.submit( () -> new BufferedReader( new InputStreamReader( process.getErrorStream(), Charsets.UTF_8 ) )
				.lines()
				.forEach( System.err::println ) );
		service.shutdown();

		Awaitility.await( process + " termination" )
				.atMost( 1, TimeUnit.MINUTES )
				.until( () -> !process.isAlive() );

		return process;
	}

	public interface SystemPropertyRestorer extends AutoCloseable {
		@Override
		void close();
	}
}
