/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.dsl.MultiFieldPredicateFieldBoostStep;

/**
 * @author Emmanuel Bernard
 */
public class FieldContext {
	private final String field;
	private boolean ignoreAnalyzer;
	private float fieldBoost = 1.0f;
	private boolean ignoreFieldBridge;

	public FieldContext(String field) {
		this.field = field;
	}

	public String getField() {
		return field;
	}

	public boolean skipAnalysis() {
		return ignoreAnalyzer;
	}

	public void setIgnoreAnalyzer(boolean ignoreAnalyzer) {
		this.ignoreAnalyzer = ignoreAnalyzer;
	}

	public <S> S applyBoost(MultiFieldPredicateFieldBoostStep<S> step) {
		return step.boost( fieldBoost );
	}

	public void boostedTo(float boost) {
		fieldBoost *= boost;
	}

	public ValueConvert getValueConvert() {
		return ignoreFieldBridge ? ValueConvert.NO : ValueConvert.YES;
	}

	public void setIgnoreFieldBridge(boolean ignoreFieldBridge) {
		this.ignoreFieldBridge = ignoreFieldBridge;
	}

	@Override
	public String toString() {
		return "FieldContext [field=" + field + ", fieldBoost=" + fieldBoost + ", ignoreAnalyzer=" + ignoreAnalyzer
				+ ", ignoreFieldBridge=" + ignoreFieldBridge + "]";
	}
}
