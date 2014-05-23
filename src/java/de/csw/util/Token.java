package de.csw.util;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 * 
 * @author ralph
 *
 */
public class Token {
	
	private CharTermAttribute charTermAttribute;
	private OffsetAttribute offsetAttribute;
	private TypeAttribute typeAttribute;
	
	public Token(CharTermAttribute charTermAttribute, OffsetAttribute offsetAttribute, TypeAttribute typeAttribute) {
		this.charTermAttribute = charTermAttribute;
		this.offsetAttribute = offsetAttribute;
		this.typeAttribute = typeAttribute;
	}

	public CharTermAttribute getCharTermAttribute() {
		return charTermAttribute;
	}

	public OffsetAttribute getOffsetAttribute() {
		return offsetAttribute;
	}
	
	public TypeAttribute getTypeAttribute() {
		return typeAttribute;
	}

	@Override
	public boolean equals(Object obj) {
		Token t = (Token)obj;
		return this.charTermAttribute.equals(t.charTermAttribute) &&
				this.offsetAttribute.equals(t.offsetAttribute) &&
				this.typeAttribute.equals(t.typeAttribute);
	}
}