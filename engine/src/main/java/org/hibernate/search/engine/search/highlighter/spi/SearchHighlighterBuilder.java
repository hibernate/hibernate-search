/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.highlighter.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterEncoder;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFragmenter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterTagSchema;

public abstract class SearchHighlighterBuilder {

	protected SearchHighlighterType type;
	protected Character[] boundaryChars;
	protected Integer boundaryMaxScan;
	protected Integer fragmentSize;
	protected Integer noMatchSize;
	protected Integer numberOfFragments;
	protected Boolean orderByScore;
	protected List<String> preTags;
	protected List<String> postTags;
	protected BoundaryScannerType boundaryScannerType = BoundaryScannerType.DEFAULT;
	protected Locale boundaryScannerLocale;
	protected HighlighterFragmenter fragmenterType;
	protected Integer phraseLimit;
	protected HighlighterEncoder encoder;
	protected HighlighterTagSchema tagSchema;

	public SearchHighlighterBuilder type(SearchHighlighterType type) {
		this.type = type;
		return this;
	}

	public SearchHighlighterBuilder boundaryChars(String boundaryChars) {
		if ( boundaryChars == null ) {
			this.boundaryChars = null;
		}
		else {
			this.boundaryChars = new Character[boundaryChars.length()];
			for ( int i = 0; i < boundaryChars.length(); i++ ) {
				this.boundaryChars[i] = Character.valueOf( boundaryChars.charAt( i ) );
			}
		}

		return this;
	}

	public SearchHighlighterBuilder boundaryChars(Character[] boundaryChars) {
		this.boundaryChars = boundaryChars;
		return this;
	}

	public SearchHighlighterBuilder boundaryMaxScan(Integer boundaryMaxScan) {
		this.boundaryMaxScan = boundaryMaxScan;
		return this;
	}

	public SearchHighlighterBuilder fragmentSize(Integer fragmentSize) {
		this.fragmentSize = fragmentSize;
		return this;
	}

	public SearchHighlighterBuilder noMatchSize(Integer noMatchSize) {
		this.noMatchSize = noMatchSize;
		return this;
	}

	public SearchHighlighterBuilder numberOfFragments(Integer numberOfFragments) {
		this.numberOfFragments = numberOfFragments;
		return this;
	}

	public SearchHighlighterBuilder orderByScore(Boolean orderByScore) {
		this.orderByScore = orderByScore;
		return this;
	}

	public SearchHighlighterBuilder boundaryScannerType(BoundaryScannerType boundaryScannerType) {
		this.boundaryScannerType = boundaryScannerType;
		return this;
	}

	public SearchHighlighterBuilder boundaryScannerLocale(Locale boundaryScannerLocale) {
		this.boundaryScannerLocale = boundaryScannerLocale;
		return this;
	}

	public SearchHighlighterBuilder fragmenter(HighlighterFragmenter type) {
		this.fragmenterType = type;
		return this;
	}

	public SearchHighlighterBuilder phraseLimit(Integer phraseLimit) {
		this.phraseLimit = phraseLimit;
		return this;
	}

	public SearchHighlighterBuilder tag(String preTag, String postTag) {
		if ( this.preTags == null ) {
			this.preTags = new ArrayList<>();
		}
		this.preTags.add( preTag );

		if ( this.postTags == null ) {
			this.postTags = new ArrayList<>();
		}
		this.postTags.add( postTag );
		return this;
	}

	public SearchHighlighterBuilder tags(Collection<String> preTags, String postTag) {
		for ( String preTag : preTags ) {
			tag( preTag, postTag );
		}
		return this;
	}

	public SearchHighlighterBuilder tags(Collection<String> preTags, Collection<String> postTags) {
		Iterator<String> pre = preTags.iterator();
		Iterator<String> post = postTags.iterator();
		while ( pre.hasNext() && post.hasNext() ) {
			tag( pre.next(), post.next() );
		}
		return this;
	}

	public void clearTags() {
		clearIfNotNull( preTags );
		clearIfNotNull( postTags );
		tagSchema( null );
	}

	private void clearIfNotNull(List<String> tags) {
		if ( tags != null ) {
			tags.clear();
		}
	}

	public SearchHighlighterBuilder tagSchema(HighlighterTagSchema tagSchema) {
		this.tagSchema = tagSchema;
		return this;
	}

	public SearchHighlighterBuilder encoder(HighlighterEncoder encoder) {
		this.encoder = encoder;
		return this;
	}

	public SearchHighlighterType type() {
		return type;
	}

	public Character[] boundaryChars() {
		return boundaryChars;
	}

	public String boundaryCharsAsString() {
		if ( boundaryChars == null ) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		for ( Character character : boundaryChars ) {
			result.append( character.charValue() );
		}

		return result.toString();
	}

	public Integer boundaryMaxScan() {
		return boundaryMaxScan;
	}

	public Integer fragmentSize() {
		return fragmentSize;
	}

	public Integer noMatchSize() {
		return noMatchSize;
	}

	public Integer numberOfFragments() {
		return numberOfFragments;
	}

	public Boolean orderByScore() {
		return orderByScore;
	}

	public List<String> preTags() {
		return preTags;
	}

	public List<String> postTags() {
		return postTags;
	}

	public BoundaryScannerType boundaryScannerType() {
		return boundaryScannerType;
	}

	public Locale boundaryScannerLocale() {
		return boundaryScannerLocale;
	}

	public HighlighterFragmenter fragmenterType() {
		return fragmenterType;
	}

	public Integer phraseLimit() {
		return phraseLimit;
	}

	public HighlighterTagSchema tagSchema() {
		return tagSchema;
	}

	public HighlighterEncoder encoder() {
		return encoder;
	}

	public abstract SearchHighlighter build();
}
