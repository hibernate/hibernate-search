/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.writer.impl;

enum TraitKind {

	UNTYPED( false, false, false ),
	TYPED_INPUT( true, true, false ),
	TYPED_OUTPUT( true, false, true );

	private final boolean requiresValueModel;
	private final boolean requiresInputType;
	private final boolean requiresOutputType;

	TraitKind(boolean requiresValueModel, boolean requiresInputType, boolean requiresOutputType) {
		this.requiresValueModel = requiresValueModel;
		this.requiresInputType = requiresInputType;
		this.requiresOutputType = requiresOutputType;
	}

	public boolean requiresValueModel() {
		return requiresValueModel;
	}

	public boolean requiresInputType() {
		return requiresInputType;
	}

	public boolean requiresOutputType() {
		return requiresOutputType;
	}
}
