/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * An interface for pluggable components that set up and tear down the environment of mass indexing threads,
 * for example, to initialize {@link ThreadLocal ThreadLocals}. See interfaces extending {@link Context} to learn which threads will
 * attempt to execute configured hooks.
 */
@Incubating
public interface MassIndexingEnvironment {
	/**
	 * Method is going to be invoked prior to executing the main logic of a {@link Runnable} in the given thread.
	 */
	void beforeExecution(Context context);

	/**
	 * Method is going to be invoked after completion of execution of the main logic of a {@link Runnable} in the given thread.
	 * Will not be called if {@link #beforeExecution(Context)} results in an exception.
	 */
	void afterExecution(Context context);

	interface Context {

		default <T> T unwrap(Class<T> contextClass) {
			return contextClass.cast( this );
		}
	}

	/**
	 * Context provided to {@link MassIndexingEnvironment} when configured hooks are considered for
	 * execution around the identifier loading work.
	 */
	interface EntityIdentifierLoadingContext extends Context {

	}

	/**
	 * Context provided to {@link MassIndexingEnvironment} when configured hooks are considered for
	 * execution around the entity loading work.
	 */
	interface EntityLoadingContext extends Context {

	}
}
