/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.highlighter.spi;

/**
 * Types of supported highlighters.
 */
public enum SearchHighlighterType {
	UNIFIED, PLAIN, FAST_VECTOR;
}
