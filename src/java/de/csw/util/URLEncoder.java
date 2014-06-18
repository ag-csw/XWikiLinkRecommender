package de.csw.util;

import java.io.UnsupportedEncodingException;

/**
 * small wrapper to ignore the UnsupportedEncodingException when URL-encoding in UTF-8
 */
public class URLEncoder {

    public static String encode(String str) {
	try {
	    return java.net.URLEncoder.encode(str, "UTF-8");
	} catch (UnsupportedEncodingException e) {
	    throw new RuntimeException("UTF-8 not found: this should not happen");
	}
    }
}
