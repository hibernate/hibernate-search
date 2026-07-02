/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Consumer;

import org.testcontainers.containers.GenericContainer;

public final class TestContainerLock {

	private TestContainerLock() {
	}

	public static void startContainersWithLock(List<? extends GenericContainer<?>> containers, Path lockFile) {
		startContainersWithLock( containers, lockFile, c -> {} );
	}

	public static void startContainersWithLock(List<? extends GenericContainer<?>> containers, Path lockFile,
			Consumer<GenericContainer<?>> onFirstStart) {
		try {
			Files.createDirectories( lockFile.getParent() );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Failed to create lock file directory: " + lockFile.getParent(), e );
		}
		try (
				FileChannel channel = FileChannel.open(
						lockFile,
						StandardOpenOption.CREATE, StandardOpenOption.WRITE
				);
				FileLock ignored = channel.lock()
		) {
			for ( GenericContainer<?> container : containers ) {
				if ( !container.isRunning() ) {
					container.start();
					onFirstStart.accept( container );
				}
			}
		}
		catch (IOException e) {
			throw new IllegalStateException( "Failed to acquire container lock: " + lockFile, e );
		}
	}
}
