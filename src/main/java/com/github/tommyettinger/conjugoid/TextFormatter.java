/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.github.tommyettinger.conjugoid;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * {@code TextFormatter} is used by {@code I18NBundle} to perform argument replacement.
 * <br>
 * Very similar to the class by the same name in libGDX, but explicitly GWT-incompatible so this can use Locales.
 * @author davebaol */
public class TextFormatter {

	private final MessageFormat messageFormat;
	private final StringBuilder sb;

	public TextFormatter() {
		this(Locale.US);
	}
	public TextFormatter(Locale locale) {
		sb = new StringBuilder();
		messageFormat = new MessageFormat("", locale);
	}

	TextFormatter(Locale locale, boolean ignored) {
		this(locale);
	}

	/** Formats the given {@code pattern} replacing its placeholders with the actual arguments specified by {@code args}.
	 * <p>
	 * If this {@code TextFormatter} has been instantiated with {@link #TextFormatter(Locale, boolean) TextFormatter(locale, true)}
	 * {@link MessageFormat} is used to process the pattern, meaning that the actual arguments are properly localized with the
	 * locale of this {@code TextFormatter}.
	 * <p>
	 * There's only one simple escaping rule, i.e. a left curly bracket must be doubled if you want it to be part of
	 * your string.
	 * <p>
	 * It's worth noting that the rules for using single quotes within {@link MessageFormat} patterns have shown to be somewhat
	 * confusing. In particular, it isn't always obvious to localizers whether single quotes need to be doubled or not. For this
	 * very reason we decided to offer the simpler escaping rule above without limiting the expressive power of message format
	 * patterns. So, if you're used to MessageFormat's syntax, remember that with {@code TextFormatter} single quotes never need to
	 * be escaped!
	 * 
	 * @param pattern the pattern
	 * @param args the arguments
	 * @return the formatted pattern
	 * @exception IllegalArgumentException if the pattern is invalid */
	public String format (String pattern, Object... args) {
		messageFormat.applyPattern(replaceEscapeChars(pattern));
		return messageFormat.format(args);
	}

	// This code is needed because a simple replacement like
	// pattern.replace("'", "''").replace("{{", "'{'");
	// can't properly manage some special cases.
	// For example, the expected output for {{{{ is {{ but you get {'{ instead.
	// Also, this code is optimized since a new string is returned only if something has been replaced.
	private String replaceEscapeChars (String pattern) {
		sb.setLength(0);
		boolean changed = false;
		int len = pattern.length();
		for (int i = 0; i < len; i++) {
			char ch = pattern.charAt(i);
			if (ch == '\'') {
				changed = true;
				sb.append("''");
			} else if (ch == '{') {
				int j = i + 1;
				while (j < len && pattern.charAt(j) == '{')
					j++;
				int escaped = (j - i) / 2;
				if (escaped > 0) {
					changed = true;
					sb.append('\'');
					do {
						sb.append('{');
					} while ((--escaped) > 0);
					sb.append('\'');
				}
				if ((j - i) % 2 != 0) sb.append('{');
				i = j - 1;
			} else {
				sb.append(ch);
			}
		}
		return changed ? sb.toString() : pattern;
	}
}
