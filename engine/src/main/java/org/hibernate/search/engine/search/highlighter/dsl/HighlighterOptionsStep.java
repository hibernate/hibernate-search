/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.highlighter.dsl;

/**
 * TODO
 */
public interface HighlighterOptionsStep<T extends HighlighterOptionsStep<?>> extends HighlighterFinalStep {

	T fragmentSize(int size);

	T noMatchSize(int size);

	T numberOfFragments(int number);

	T orderByScore(boolean enable);

	T tag(String preTag, String postTag);

	T encoder(HighlighterEncoder encoder);

}
