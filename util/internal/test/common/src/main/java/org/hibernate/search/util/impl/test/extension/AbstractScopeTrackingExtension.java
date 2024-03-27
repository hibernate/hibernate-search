/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.jboss.logging.Logger;

public abstract class AbstractScopeTrackingExtension
		implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback, AfterEachCallback {
	private static final Logger log = Logger.getLogger( AbstractScopeTrackingExtension.class.getName() );

	private ExtensionScope scope = ExtensionScope.CLASS;

	protected final void updateScope(ExtensionScope scope) {
		if ( !this.scope.equals( scope ) ) {
			log.debug( "Scope changed to: " + scope );
			this.scope = scope;
		}
	}

	protected final ExtensionScope currentScope() {
		return scope;
	}

	@Override
	public final void beforeAll(ExtensionContext extensionContext) throws Exception {
		updateScope( ExtensionScope.CLASS );
		actualBeforeAll( extensionContext );
	}

	protected void actualBeforeAll(ExtensionContext extensionContext) throws Exception {
	}

	@Override
	public final void beforeEach(ExtensionContext extensionContext) throws Exception {
		updateScope( ExtensionScope.TEST );
		actualBeforeEach( extensionContext );
	}

	protected void actualBeforeEach(ExtensionContext extensionContext) throws Exception {
	}

	@Override
	public final void afterAll(ExtensionContext extensionContext) throws Exception {
		updateScope( ExtensionScope.CLASS );
		actualAfterAll( extensionContext );
	}

	protected void actualAfterAll(ExtensionContext extensionContext) throws Exception {
	}

	@Override
	public final void afterEach(ExtensionContext extensionContext) throws Exception {
		updateScope( ExtensionScope.TEST );
		actualAfterEach( extensionContext );
	}

	protected void actualAfterEach(ExtensionContext extensionContext) throws Exception {
	}
}
