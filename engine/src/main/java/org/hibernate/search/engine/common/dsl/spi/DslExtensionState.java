/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.dsl.spi;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A utility class holding the state of the extension contexts found in several DSLs.
 *
 * @param <R> The result type to expect from functions applied to extended contexts.
 */
public final class DslExtensionState<R> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static <E> E returnIfSupported(Object extension, Optional<E> extendedContextOptional) {
		DslExtensionState<E> state = new DslExtensionState<>();
		state.ifSupported( extension, extendedContextOptional, context -> context );
		return state.orElseFail();
	}

	private boolean appliedAtLeastOneExtension = false;
	private boolean appliedOrElse = false;

	private R result = null;

	private List<Object> unsupportedExtensions;

	public <E> void ifSupported(Object extension, Optional<E> extendedContextOptional,
			Function<E, ? extends R> extendedContextFunction) {
		if ( appliedOrElse ) {
			throw log.cannotCallDslExtensionIfSupportedAfterOrElse();
		}
		if ( !appliedAtLeastOneExtension ) {
			if ( extendedContextOptional.isPresent() ) {
				appliedAtLeastOneExtension = true;
				result = extendedContextFunction.apply( extendedContextOptional.get() );
			}
			else {
				if ( unsupportedExtensions == null ) {
					unsupportedExtensions = new ArrayList<>();
				}
				unsupportedExtensions.add( extension );
			}
		}
	}

	public <T> R orElse(T defaultContext, Consumer<T> defaultContextConsumer) {
		return orElse( defaultContext, c -> {
			defaultContextConsumer.accept( c );
			return null;
		} );
	}

	public <T> R orElse(T defaultContext, Function<T, ? extends R> defaultContextFunction) {
		if ( !appliedAtLeastOneExtension ) {
			appliedOrElse = true;
			result = defaultContextFunction.apply( defaultContext );
		}
		return result;
	}

	public R orElseFail() {
		if ( !appliedAtLeastOneExtension ) {
			appliedOrElse = true;
			throw log.dslExtensionNoMatch( unsupportedExtensions == null ? Collections.emptyList() : unsupportedExtensions );
		}
		return result;
	}

}
