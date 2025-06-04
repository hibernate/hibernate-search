/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.util.impl;

import java.lang.reflect.Field;

public final class DefaultBackendVersionUtils {
	private DefaultBackendVersionUtils() {
	}

	public static String latestElasticsearchVersion() {
		// This implementation is replaced during the build with another one that returns the correct value:
		return "UNKNOWN";
	}

	public static String latestLuceneVersion() {
		// since this one depends on which backend we pass to the processor ...
		try {
			Class<?> luceneVersionClass = Class.forName( "org.apache.lucene.util.Version" );
			Field latestField = luceneVersionClass.getField( "LATEST" );

			Object latestVersion = latestField.get( null );

			return latestVersion.toString();
		}
		catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
			return null;
		}
	}
}
