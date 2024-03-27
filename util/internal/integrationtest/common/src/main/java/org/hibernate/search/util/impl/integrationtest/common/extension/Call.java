/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;

import org.hibernate.search.util.common.spi.ToStringTreeAppendable;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

public abstract class Call<S extends Call<S>> implements ToStringTreeAppendable {

	@Override
	public final String toString() {
		return summary();
	}

	@Override
	public final void appendTo(ToStringTreeAppender appender) {
		details( appender );
	}

	/**
	 * @param other Another call of similar type.
	 * @return {@code true} if the other call is similar, i.e. a first cursory check indicates it could be identical.
	 * {@code false} if there is no chance it could be identical.
	 */
	protected abstract boolean isSimilarTo(S other);

	protected abstract String summary();

	protected void details(ToStringTreeAppender appender) {
		// No details by default.
	}

}
