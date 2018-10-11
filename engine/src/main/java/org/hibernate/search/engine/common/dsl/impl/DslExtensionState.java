/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * A utility class holding the state of the extension contexts found in several DSLs.
 */
public final class DslExtensionState {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static <E> E returnIfSupported(Object extension, Optional<E> extendedContextOptional) {
		DslExtensionState state = new DslExtensionState();
		state.ifSupported( extension, extendedContextOptional, ignored -> { } );
		state.orElseFail();
		// If we reach this line, the optional was not empty
		return extendedContextOptional.get();
	}

	private boolean appliedAtLeastOneExtension = false;
	private boolean appliedOrElse = false;

	private List<Object> unsupportedExtensions;

	public <E> void ifSupported(Object extension, Optional<E> extendedContextOptional, Consumer<E> extendedContextConsumer) {
		if ( appliedOrElse ) {
			throw log.cannotCallDslExtensionIfSupportedAfterOrElse();
		}
		if ( !appliedAtLeastOneExtension ) {
			if ( extendedContextOptional.isPresent() ) {
				appliedAtLeastOneExtension = true;
				extendedContextConsumer.accept( extendedContextOptional.get() );
			}
			else {
				if ( unsupportedExtensions == null ) {
					unsupportedExtensions = new ArrayList<>();
				}
				unsupportedExtensions.add( extension );
			}
		}
	}

	public <T> void orElse(T defaultContext, Consumer<T> defaultContextConsumer) {
		if ( !appliedAtLeastOneExtension ) {
			appliedOrElse = true;
			defaultContextConsumer.accept( defaultContext );
		}
	}

	public void orElseFail() {
		if ( !appliedAtLeastOneExtension ) {
			appliedOrElse = true;
			throw log.dslExtensionNoMatch( unsupportedExtensions == null ? Collections.emptyList() : unsupportedExtensions );
		}
	}

}
