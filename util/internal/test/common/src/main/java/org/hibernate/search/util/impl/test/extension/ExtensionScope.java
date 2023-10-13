/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test.extension;

import java.util.function.Consumer;

import org.hibernate.search.util.impl.test.function.ThrowingConsumer;

import org.junit.jupiter.api.extension.ExtensionContext;

import org.jboss.logging.Logger;

public enum ExtensionScope {

	CLASS,
	PARAMETERIZED_CLASS_SETUP,
	TEST;

	private static final Logger log = Logger.getLogger( ExtensionScope.class.getName() );

	public static void currentScopeModifier(ExtensionContext extensionContext, Consumer<ExtensionScope> modifier) {
		extensionContext
				.getStore( namespace( extensionContext ) )
				.put( "modifier", modifier );
	}

	@SuppressWarnings("unchecked")
	public static Consumer<ExtensionScope> currentScopeModifier(ExtensionContext extensionContext) {
		return extensionContext
				.getStore( namespace( extensionContext ) )
				.getOrDefault( "modifier", Consumer.class, scope -> log.warn( "No scope modifier found." ) );
	}

	public static void scopeCleanUp(ExtensionContext extensionContext, ThrowingConsumer<ExtensionScope, Exception> cleanup) {
		extensionContext
				.getStore( namespace( extensionContext ) )
				.put( "cleanup", cleanup );
	}

	@SuppressWarnings("unchecked")
	public static ThrowingConsumer<ExtensionScope, Exception> scopeCleanUp(ExtensionContext extensionContext) {
		return extensionContext
				.getStore( namespace( extensionContext ) )
				.getOrDefault( "cleanup", ThrowingConsumer.class, scope -> log.warn( "No scope clean up found." ) );
	}

	private static ExtensionContext.Namespace namespace(ExtensionContext extensionContext) {
		return ExtensionContext.Namespace.create( extensionContext.getRequiredTestClass(), ExtensionScope.class );
	}
}
