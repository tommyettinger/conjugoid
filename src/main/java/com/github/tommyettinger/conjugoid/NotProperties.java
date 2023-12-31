/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.tommyettinger.conjugoid;

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.ds.ObjectObjectMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.Map.Entry;

/**
 *  {@code NotProperties} is a helper class that allows you to load and store key/value pairs of an
 * {@code ObjectObjectMap<String,String>} with the same line-oriented syntax supported by
 * {@code java.util.Properties}, except unlike a .properties file, you can use Unicode text here. Hallelujah!
 * If you pass an {@link com.github.tommyettinger.ds.ObjectObjectOrderedMap} to {@link #load(ObjectObjectMap, Reader)}
 * or {@link #store(ObjectObjectMap, Writer, String)}, this will also preserve the order of keys in an ObjectList, in
 * case you need that.
 */
public final class NotProperties {

	private static final int NONE = 0, SLASH = 1, UNICODE = 2, CONTINUE = 3, KEY_DONE = 4, IGNORE = 5;

	private static final String LINE_SEPARATOR = "\n";

	private NotProperties() {
	}

	/** Adds to the specified {@code ObjectObjectMap} the key/value pairs loaded from the {@code Reader} in a simple line-oriented format
	 * compatible with {@code java.util.Properties}, but Unicode-capable and order-preserving.
	 * <p>
	 * The input stream remains open after this method returns.
	 *
	 * @param properties the map to be filled.
	 * @param reader the input character stream reader.
	 * @throws IOException if an error occurred when reading from the input stream.
	 * @throws IllegalArgumentException if a malformed Unicode escape appears in the input. */
	public static void load (ObjectObjectMap<String, String> properties, Reader reader) throws IOException {
		if (properties == null) throw new NullPointerException("properties cannot be null");
		if (reader == null) throw new NullPointerException("reader cannot be null");
		int mode = NONE, unicode = 0, count = 0;
		char nextChar, buf[] = new char[40];
		int offset = 0, keyLength = -1, intVal;
		boolean firstChar = true;

		BufferedReader br = new BufferedReader(reader);

		while (true) {
			intVal = br.read();
			if (intVal == -1) {
				break;
			}
			nextChar = (char)intVal;

			if (offset == buf.length) {
				char[] newBuf = new char[buf.length * 2];
				System.arraycopy(buf, 0, newBuf, 0, offset);
				buf = newBuf;
			}
			if (mode == UNICODE) {
				int digit = Character.digit(nextChar, 16);
				if (digit >= 0) {
					unicode = (unicode << 4) + digit;
					if (++count < 4) {
						continue;
					}
				} else if (count <= 4) {
					throw new IllegalArgumentException("Invalid Unicode sequence: illegal character");
				}
				mode = NONE;
				buf[offset++] = (char)unicode;
				if (nextChar != '\n') {
					continue;
				}
			}
			if (mode == SLASH) {
				mode = NONE;
				switch (nextChar) {
				case '\r':
					mode = CONTINUE; // Look for a following \n
					continue;
				case '\n':
					mode = IGNORE; // Ignore whitespace on the next line
					continue;
				case 'b':
					nextChar = '\b';
					break;
				case 'f':
					nextChar = '\f';
					break;
				case 'n':
					nextChar = '\n';
					break;
				case 'r':
					nextChar = '\r';
					break;
				case 't':
					nextChar = '\t';
					break;
				case 'u':
					mode = UNICODE;
					unicode = count = 0;
					continue;
				}
			} else {
				switch (nextChar) {
				case '#':
				case '!':
					if (firstChar) {
						while (true) {
							intVal = br.read();
							if (intVal == -1) {
								break;
							}
							nextChar = (char)intVal;
							if (nextChar == '\r' || nextChar == '\n') {
								break;
							}
						}
						continue;
					}
					break;
				case '\n':
					if (mode == CONTINUE) { // Part of a \r\n sequence
						mode = IGNORE; // Ignore whitespace on the next line
						continue;
					}
					// fall into the next case
				case '\r':
					mode = NONE;
					firstChar = true;
					if (offset > 0 || (offset == 0 && keyLength == 0)) {
						if (keyLength == -1) {
							keyLength = offset;
						}
						String temp = new String(buf, 0, offset);
						properties.put(temp.substring(0, keyLength), temp.substring(keyLength));
					}
					keyLength = -1;
					offset = 0;
					continue;
				case '\\':
					if (mode == KEY_DONE) {
						keyLength = offset;
					}
					mode = SLASH;
					continue;
				case '=':
					if (keyLength == -1) { // if parsing the key
						mode = NONE;
						keyLength = offset;
						continue;
					}
					break;
				}
				if (nextChar == ' ' || nextChar == '\t' || nextChar == '\f') {
					if (mode == CONTINUE) {
						mode = IGNORE;
					}
					// if key length == 0 or value length == 0
					if (offset == 0 || offset == keyLength || mode == IGNORE) {
						continue;
					}
					if (keyLength == -1) { // if parsing the key
						mode = KEY_DONE;
						continue;
					}
				}
				if (mode == IGNORE || mode == CONTINUE) {
					mode = NONE;
				}
			}
			firstChar = false;
			if (mode == KEY_DONE) {
				keyLength = offset;
				mode = NONE;
			}
			buf[offset++] = nextChar;
		}
		if (mode == UNICODE && count <= 4) {
			throw new IllegalArgumentException("Invalid Unicode sequence: expected format \\uxxxx");
		}
		if (keyLength == -1 && offset > 0) {
			keyLength = offset;
		}
		if (keyLength >= 0) {
			String temp = new String(buf, 0, offset);
			String key = temp.substring(0, keyLength);
			String value = temp.substring(keyLength);
			if (mode == SLASH) {
				value += "\u0000";
			}
			properties.put(key, value);
		}
	}

	/** Writes the key/value pairs of the specified {@code ObjectObjectMap} to the output character stream in a
	 *  simple line-oriented format compatible with {@code java.util.Properties}, but Unicode-capable and order-preserving.
	 * <p>
	 * Every entry in the {@code java.util.Properties} is written out, one per line. For each entry the key string is written, then an
	 * ASCII {@code =}, then the associated element string. For the key, all space characters are written with a preceding
	 * {@code \} character. For the element, leading space characters, but not embedded or trailing space characters, are
	 * written with a preceding {@code \} character.
	 * The key and element character {@code =} is written with a preceding backslash to ensure that it is properly
	 * loaded, as are the characters {@code #} and {@code !} when they start a line.
	 * <p>
	 * After the entries have been written, the output stream is flushed. The output stream remains open after this method returns.
	 *
	 * @param properties the {@code ObjectObjectMap}.
	 * @param writer an output character stream writer.
	 * @param comment an optional comment to be written, or null.
	 * @exception IOException if writing this property list to the specified output stream throws an {@code IOException}.
	 * @exception NullPointerException if {@code writer} is null. */
	public static void store (ObjectObjectMap<String, String> properties, Writer writer, String comment) throws IOException {
		storeImpl(properties, writer, comment);
	}

	private static void storeImpl (ObjectObjectMap<String, String> properties, Writer writer, String comment)
		throws IOException {
		if (comment != null) {
			writeComment(writer, comment);
		}
		writer.write("#");
		writer.write(new Date().toString());
		writer.write(LINE_SEPARATOR);

		StringBuilder sb = new StringBuilder(200);
		for (Entry<String, String> entry : properties.entrySet()) {
			dumpString(sb, entry.getKey(), true);
			sb.append('=');
			dumpString(sb, entry.getValue(), false);
			writer.write(LINE_SEPARATOR);
			writer.write(sb.toString());
			sb.setLength(0);
		}
		writer.flush();
	}

	private static void dumpString (StringBuilder outBuffer, String string, boolean isKey) {
		int len = string.length();
		for (int i = 0; i < len; i++) {
			char ch = string.charAt(i);
			// Handle common case first
			if ((ch > 61) && (ch <= 126)) {
				outBuffer.append(ch == '\\' ? "\\\\" : ch);
				continue;
			}
			switch (ch) {
				case ' ':
					if (i == 0 || isKey) {
						outBuffer.append("\\ ");
					} else {
						outBuffer.append(ch);
					}
					break;
				case '\n':
					outBuffer.append("\\n");
					break;
				case '\r':
					outBuffer.append("\\r");
					break;
				case '\t':
					outBuffer.append("\\t");
					break;
				case '\f':
					outBuffer.append("\\f");
					break;
				case '#': // Fall through
				case '!':
                    if (i == 0 && isKey)
                        outBuffer.append('\\');
					outBuffer.append(ch);
                    break;
				case '=':
					outBuffer.append('\\').append(ch);
					break;
				default:
					if ((ch < 32)) {
						outBuffer.append("\\u");
						Base.BASE16.appendUnsigned(outBuffer, ch);
					} else {
						outBuffer.append(ch);
					}
					break;
			}
		}
	}

	private static void writeComment (Writer writer, String comment) throws IOException {
		writer.write("#");
		int len = comment.length();
		int curIndex = 0;
		int lastIndex = 0;
		while (curIndex < len) {
			char c = comment.charAt(curIndex);
			if (c == '\n' || c == '\r') {
				if (lastIndex != curIndex) writer.write(comment.substring(lastIndex, curIndex));

				writer.write(LINE_SEPARATOR);
				if (c == '\r' && curIndex != len - 1 && comment.charAt(curIndex + 1) == '\n') {
					curIndex++;
				}
				if (curIndex == len - 1 || (comment.charAt(curIndex + 1) != '#' && comment.charAt(curIndex + 1) != '!'))
					writer.write("#");

				lastIndex = curIndex + 1;
			}
			curIndex++;
		}
		if (lastIndex != curIndex) writer.write(comment.substring(lastIndex, curIndex));
		writer.write(LINE_SEPARATOR);
	}
}
