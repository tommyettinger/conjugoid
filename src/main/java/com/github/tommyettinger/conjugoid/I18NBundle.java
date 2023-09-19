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

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;
import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.ObjectObjectOrderedMap;
import com.github.tommyettinger.ds.ObjectOrderedSet;

import java.io.IOException;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

/** A {@code I18NBundle} provides {@code Locale}-specific resources loaded from property files. A bundle contains a number of
 * named resources, whose names and values are {@code Strings}. A bundle may have a parent bundle, and when a resource is not
 * found in a bundle, the parent bundle is searched for the resource. If the fallback mechanism reaches the base bundle and still
 * can't find the resource it throws a {@code MissingResourceException}.
 * 
 * <ul>
 * <li>All bundles for the same group of resources share a common base bundle. This base bundle acts as the root and is the last
 * fallback in case none of its children was able to respond to a request.</li>
 * <li>The first level contains changes between different languages. Only the differences between a language and the language of
 * the base bundle need to be handled by a language-specific {@code I18NBundle}.</li>
 * <li>The second level contains changes between different countries that use the same language. Only the differences between a
 * country and the country of the language bundle need to be handled by a country-specific {@code I18NBundle}.</li>
 * <li>The third level contains changes that don't have a geographic reason (e.g. changes that where made at some point in time
 * like {@code PREEURO} where the currency of come countries changed. The country bundle would return the current currency (Euro)
 * and the {@code PREEURO} variant bundle would return the old currency (e.g. DM for Germany).</li>
 * </ul>
 * 
 * <strong>Examples</strong>
 * <ul>
 * <li>BaseName (base bundle)
 * <li>BaseName_de (german language bundle)
 * <li>BaseName_fr (french language bundle)
 * <li>BaseName_de_DE (bundle with Germany specific resources in german)
 * <li>BaseName_de_CH (bundle with Switzerland specific resources in german)
 * <li>BaseName_fr_CH (bundle with Switzerland specific resources in french)
 * <li>BaseName_de_DE_PREEURO (bundle with Germany specific resources in german of the time before the Euro)
 * <li>BaseName_fr_FR_PREEURO (bundle with France specific resources in french of the time before the Euro)
 * </ul>
 * 
 * It's also possible to create variants for languages or countries. This can be done by just skipping the country or language
 * abbreviation: BaseName_us__POSIX or BaseName__DE_PREEURO. But it's not allowed to circumvent both language and country:
 * BaseName___VARIANT is illegal.
 * 
 * @see NotProperties
 * 
 * @author davebaol */
@SuppressWarnings("Java8MapApi")
public class I18NBundle {

	private static final String DEFAULT_ENCODING = "UTF-8";

	// Locale.ROOT does not exist in Android API level 8
	private static final Locale ROOT_LOCALE = new Locale("", "", "");

	private static boolean exceptionOnMissingKey = true;

	/** The parent of this {@code I18NBundle} that is used if this bundle doesn't include the requested resource. */
	private I18NBundle parent;

	/** The locale for this bundle. */
	private Locale locale;

	/** The properties for this bundle. */
	private ObjectObjectOrderedMap<String, String> properties;

	private MessageFormat messageFormat;
	private final StringBuilder sb = new StringBuilder();


	/** Returns the flag indicating whether to throw a {@link MissingResourceException} from the {@link #get(String) get(key)}
	 * method if no string for the given key can be found. If this flag is {@code false} the missing key surrounded by {@code ???}
	 * is returned. */
	public static boolean getExceptionOnMissingKey () {
		return exceptionOnMissingKey;
	}

	/** Sets the flag indicating whether to throw a {@link MissingResourceException} from the {@link #get(String) get(key)} method
	 * if no string for the given key can be found. If this flag is {@code false} the missing key surrounded by {@code ???} is
	 * returned. */
	public static void setExceptionOnMissingKey (boolean enabled) {
		exceptionOnMissingKey = enabled;
	}

	/** Creates a new bundle using the specified {@code baseFileHandle}, the default locale and the default encoding "UTF-8".
	 * 
	 * @param baseFileHandle the file handle to the base of the bundle
	 * @exception NullPointerException if {@code baseFileHandle} is {@code null}
	 * @exception MissingResourceException if no bundle for the specified base file handle can be found
	 * @return a bundle for the given base file handle and the default locale */
	public static I18NBundle createBundle (FileHandle baseFileHandle) {
		return createBundleImpl(baseFileHandle, Locale.getDefault(), DEFAULT_ENCODING);
	}

	/** Creates a new bundle using the specified {@code baseFileHandle} and {@code locale}; the default encoding "UTF-8"
	 * is used.
	 * 
	 * @param baseFileHandle the file handle to the base of the bundle
	 * @param locale the locale for which a bundle is desired
	 * @return a bundle for the given base file handle and locale
	 * @exception NullPointerException if {@code baseFileHandle} or {@code locale} is {@code null}
	 * @exception MissingResourceException if no bundle for the specified base file handle can be found */
	public static I18NBundle createBundle (FileHandle baseFileHandle, Locale locale) {
		return createBundleImpl(baseFileHandle, locale, DEFAULT_ENCODING);
	}

	/** Creates a new bundle using the specified {@code baseFileHandle} and {@code encoding}; the default locale is used.
	 * 
	 * @param baseFileHandle the file handle to the base of the bundle
	 * @param encoding the character encoding
	 * @return a bundle for the given base file handle and locale
	 * @exception NullPointerException if {@code baseFileHandle} or {@code encoding} is {@code null}
	 * @exception MissingResourceException if no bundle for the specified base file handle can be found */
	public static I18NBundle createBundle (FileHandle baseFileHandle, String encoding) {
		return createBundleImpl(baseFileHandle, Locale.getDefault(), encoding);
	}

	/** Creates a new bundle using the specified {@code baseFileHandle}, {@code locale} and {@code encoding}.
	 * 
	 * @param baseFileHandle the file handle to the base of the bundle
	 * @param locale the locale for which a bundle is desired
	 * @param encoding the character encoding
	 * @return a bundle for the given base file handle and locale
	 * @exception NullPointerException if {@code baseFileHandle}, {@code locale} or {@code encoding} is
	 *               {@code null}
	 * @exception MissingResourceException if no bundle for the specified base file handle can be found */
	public static I18NBundle createBundle (FileHandle baseFileHandle, Locale locale, String encoding) {
		return createBundleImpl(baseFileHandle, locale, encoding);
	}

	private static I18NBundle createBundleImpl (FileHandle baseFileHandle, Locale locale, String encoding) {
		if (baseFileHandle == null || locale == null || encoding == null) throw new NullPointerException();

		I18NBundle bundle;
		I18NBundle baseBundle = null;
		Locale targetLocale = locale;
		do {
			// Create the candidate locales
			List<Locale> candidateLocales = getCandidateLocales(targetLocale);

			// Load the bundle and its parents recursively
			bundle = loadBundleChain(baseFileHandle, encoding, candidateLocales, 0, baseBundle);

			// Check the loaded bundle (if any)
			if (bundle != null) {
				Locale bundleLocale = bundle.locale;
				boolean isBaseBundle = bundleLocale.equals(ROOT_LOCALE);

				if (!isBaseBundle || bundleLocale.equals(locale)) {
					// Found the bundle for the requested locale
					break;
				}
				if (candidateLocales.size() == 1 && bundleLocale.equals(candidateLocales.get(0))) {
					// Found the bundle for the only candidate locale
					break;
				}
				if (baseBundle == null) {
					// Store the base bundle and keep on processing the remaining fallback locales
					baseBundle = bundle;
				}
			}

			// Set next fallback locale
			targetLocale = getFallbackLocale(targetLocale);

		} while (targetLocale != null);

		if (bundle == null) {
			if (baseBundle == null) {
				// No bundle found
				throw new MissingResourceException(
					"Can't find bundle for base file handle " + baseFileHandle.path() + ", locale " + locale,
					baseFileHandle + "_" + locale, "");
			}
			// Set the base bundle to be returned
			bundle = baseBundle;
		}

		return bundle;
	}

	/** Returns a {@code List} of {@code Locale}s as candidate locales for the given {@code locale}. This method is
	 * called by the {@code createBundle} factory method each time the factory method tries finding a resource bundle for a
	 * target {@code Locale}.
	 * <br>
	 * The sequence of the candidate locales also corresponds to the runtime resource lookup path (also known as the <i>parent
	 * chain</i>), if the corresponding resource bundles for the candidate locales exist and their parents are not defined by
	 * loaded resource bundles themselves. The last element of the list is always the {@linkplain Locale#ROOT root locale}, meaning
	 * that the base bundle is the terminal of the parent chain.
	 * <br>
	 * If the given locale is equal to {@code Locale.ROOT} (the root locale), a {@code List} containing only the root
	 * {@code Locale} is returned. In this case, the {@code createBundle} factory method loads only the base bundle as
	 * the resulting resource bundle.
	 * <br>
	 * This implementation returns a {@code List} containing {@code Locale}s in the following sequence:
	 *
	 * <pre>
	 *     Locale(language, country, variant)
	 *     Locale(language, country)
	 *     Locale(language)
	 *     Locale.ROOT
	 * </pre>
	 * 
	 * where {@code language}, {@code country} and {@code variant} are the language, country and variant values of
	 * the given {@code locale}, respectively. Locales where the final component values are empty strings are omitted.
	 * <br>
	 * For example, if the given base name is "Messages" and the given {@code locale} is
	 * {@code Locale("ja", "", "XX")}, then a {@code List} of {@code Locale}s:
	 * 
	 * <pre>
	 *     Locale("ja", "", "XX")
	 *     Locale("ja")
	 *     Locale.ROOT
	 * </pre>
	 * 
	 * is returned. And if the resource bundles for the "ja" and "" {@code Locale}s are found, then the runtime resource
	 * lookup path (parent chain) is:
	 * 
	 * <pre>
	 *     Messages_ja -> Messages
	 * </pre>
	 * 
	 * @param locale the locale for which a resource bundle is desired
	 * @return a {@code List} of candidate {@code Locale}s for the given {@code locale}
	 * @exception NullPointerException if {@code locale} is {@code null} */
	private static List<Locale> getCandidateLocales (Locale locale) {
		String language = locale.getLanguage();
		String country = locale.getCountry();
		String variant = locale.getVariant();

		List<Locale> locales = new ObjectList<>(4);
		if (!variant.isEmpty()) {
			locales.add(locale);
		}
		if (!country.isEmpty()) {
			locales.add(locales.isEmpty() ? locale : new Locale(language, country));
		}
		if (!language.isEmpty()) {
			locales.add(locales.isEmpty() ? locale : new Locale(language));
		}
		locales.add(ROOT_LOCALE);
		return locales;
	}

	/** Returns a {@code Locale} to be used as a fallback locale for further bundle searches by the {@code createBundle}
	 * factory method. This method is called from the factory method every time when no resulting bundle has been found for
	 * {@code baseFileHandler} and {@code locale}, where locale is either the parameter for {@code createBundle} or
	 * the previous fallback locale returned by this method.
	 * <br>
	 * This method returns the {@linkplain Locale#getDefault() default {@code Locale}} if the given {@code locale} isn't
	 * the default one. Otherwise, {@code null} is returned.
	 * 
	 * @param locale the {@code Locale} for which {@code createBundle} has been unable to find any resource bundles
	 *           (except for the base bundle)
	 * @return a {@code Locale} for the fallback search, or {@code null} if no further fallback search is needed.
	 * @exception NullPointerException if {@code locale} is {@code null} */
	private static Locale getFallbackLocale (Locale locale) {
		Locale defaultLocale = Locale.getDefault();
		return locale.equals(defaultLocale) ? null : defaultLocale;
	}

	private static I18NBundle loadBundleChain (FileHandle baseFileHandle, String encoding, List<Locale> candidateLocales,
		int candidateIndex, I18NBundle baseBundle) {
		Locale targetLocale = candidateLocales.get(candidateIndex);
		I18NBundle parent = null;
		if (candidateIndex != candidateLocales.size() - 1) {
			// Load recursively the parent having the next candidate locale
			parent = loadBundleChain(baseFileHandle, encoding, candidateLocales, candidateIndex + 1, baseBundle);
		} else if (baseBundle != null && targetLocale.equals(ROOT_LOCALE)) {
			return baseBundle;
		}

		// Load the bundle
		I18NBundle bundle = loadBundle(baseFileHandle, encoding, targetLocale);
		if (bundle != null) {
			bundle.parent = parent;
			return bundle;
		}

		return parent;
	}

	// Tries to load the bundle for the given locale.
	private static I18NBundle loadBundle (FileHandle baseFileHandle, String encoding, Locale targetLocale) {
		I18NBundle bundle = null;
		Reader reader = null;
		try {
			FileHandle fileHandle = toFileHandle(baseFileHandle, targetLocale);
			if (BetaTools.fileExists(fileHandle)) {
				// Instantiate the bundle
				bundle = new I18NBundle();

				// Load bundle properties from the stream with the specified encoding
				reader = fileHandle.reader(encoding);
				bundle.load(reader);
			}
		} catch (IOException e) {
			throw new GdxRuntimeException(e);
		} finally {
			StreamUtils.closeQuietly(reader);
		}
		if (bundle != null) {
			bundle.setLocale(targetLocale);
		}

		return bundle;
	}

	/** Load the properties from the specified reader.
	 * 
	 * @param reader the reader
	 * @throws IOException if an error occurred when reading from the input stream. */
	private void load (Reader reader) throws IOException {
		properties = new ObjectObjectOrderedMap<>();
		NotProperties.load(properties, reader);
	}

	/** Converts the given {@code baseFileHandle} and {@code locale} to the corresponding file handle.
	 * <br>
	 * This implementation returns the {@code baseFileHandle}'s sibling with following value:
	 * <br>
	 * {@code baseFileHandle.name() + "_" + language + "_" + country + "_" + variant + ".txt"}
	 * <br>
	 * where {@code language}, {@code country} and {@code variant} are the language, country and variant values of
	 * {@code locale}, respectively. Final component values that are empty Strings are omitted along with the preceding '_'.
	 * If all the values are empty strings, then {@code baseFileHandle.name()} is returned with ".txt" appended.
	 * 
	 * @param baseFileHandle the file handle to the base of the bundle
	 * @param locale the locale for which a resource bundle should be loaded
	 * @return the file handle for the bundle
	 * @exception NullPointerException if {@code baseFileHandle} or {@code locale} is {@code null} */
	private static FileHandle toFileHandle (FileHandle baseFileHandle, Locale locale) {
		StringBuilder sb = new StringBuilder(baseFileHandle.name());
		if (!locale.equals(ROOT_LOCALE)) {
			String language = locale.getLanguage();
			String country = locale.getCountry();
			String variant = locale.getVariant();
			boolean emptyLanguage = "".equals(language);
			boolean emptyCountry = "".equals(country);
			boolean emptyVariant = "".equals(variant);

			if (!(emptyLanguage && emptyCountry && emptyVariant)) {
				sb.append('_');
				if (!emptyVariant) {
					sb.append(language).append('_').append(country).append('_').append(variant);
				} else if (!emptyCountry) {
					sb.append(language).append('_').append(country);
				} else {
					sb.append(language);
				}
			}
		}
		return baseFileHandle.sibling(sb.append(".txt").toString());
	}

	/** Returns the locale of this bundle. This method can be used after a call to {@code createBundle()} to determine whether
	 * the resource bundle returned really corresponds to the requested locale or is a fallback.
	 * 
	 * @return the locale of this bundle */
	public Locale getLocale () {
		return locale;
	}

	/**
	 * Sets the bundle locale. This method is private because a bundle can't change the locale during its life.
	 * 
	 * @param locale a non-null Locale for this to use for everything from now on
	 */
	private void setLocale (Locale locale) {
		this.locale = locale;
		this.messageFormat = new MessageFormat("", locale);
	}

	/** Gets a string for the given key from this bundle or one of its parents.
	 * 
	 * @param key the key for the desired string
	 * @exception NullPointerException if {@code key} is {@code null}
	 * @exception MissingResourceException if no string for the given key can be found and {@link #getExceptionOnMissingKey()}
	 *               returns {@code true}
	 * @return the string for the given key or the key surrounded by {@code ???} if it cannot be found and
	 *         {@link #getExceptionOnMissingKey()} returns {@code false} */
	public String get (String key) {
		String result = properties.get(key);
		if (result == null) {
			if (parent != null) result = parent.get(key);
			if (result == null) {
				if (exceptionOnMissingKey)
					throw new MissingResourceException("Can't find bundle key " + key, this.getClass().getName(), key);
				else
					return "???" + key + "???";
			}
		}
		return result;
	}

	/** Gets an ordered key set of loaded properties. The keys from all loaded properties will be copied into a new
	 *  {@link ObjectOrderedSet} and returned.
	 *
	 * @return a key set of loaded properties. Never null, might be an empty set */
	public ObjectOrderedSet<String> keys () {
		return new ObjectOrderedSet<>(properties.keySet());
	}

	/** Gets the string with the specified key from this bundle or one of its parent after replacing the given arguments if they
	 * occur.
	 * 
	 * @param key the key for the desired string
	 * @param args the arguments to be replaced in the string associated to the given key.
	 * @exception NullPointerException if {@code key} is {@code null}
	 * @exception MissingResourceException if no string for the given key can be found
	 * @return the string for the given key formatted with the given arguments */
	public String format (String key, Object... args) {
		messageFormat.applyPattern(replaceEscapeChars(get(key)));
		return messageFormat.format(args);
	}


	// Below is roughly equivalent to:
	// pattern.replaceAll("'|(\\{+)\\1", "'$1'");
	// The approach below doesn't allocate if there was no match, so it is better here.
	/**
	 * Handles the different escape character syntax for messages here vs. those in MessageFormat. That is, here a left
	 * curly bracket must be doubled to have its intended meaning, and single quotes can be left as-is. This has a
	 * useful optimization in that it doesn't create a new String if nothing would change.
	 * @param pattern a message formatted using the rules of this class (repeat left curly bracket)
	 * @return a message formatted using MessageFormat's rules (curly braces in single quotes, single quotes doubled)
	 */
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
				if ((j - i & 1) != 0) sb.append('{');
				i = j - 1;
			} else {
				sb.append(ch);
			}
		}
		return changed ? sb.toString() : pattern;
	}

	/** Sets the value of all localized strings to String placeholder so hardcoded, unlocalized values can be easily spotted. The
	 * I18NBundle won't be able to reset values after calling debug and should only be using during testing.
	 * 
	 * @param placeholder the String that will replace every value in the loaded properties
	 */
	public void debug (String placeholder) {
		for (String s : properties.keySet()) {
			properties.put(s, placeholder);
		}
	}
}
