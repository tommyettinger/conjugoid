/*
 * Copyright (c) 2022-2023 See AUTHORS file.
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
 *
 */

package com.github.tommyettinger.conjugoid;

import com.github.tommyettinger.ds.ObjectObjectMap;
import com.github.tommyettinger.ds.ObjectObjectOrderedMap;
import com.github.tommyettinger.ds.Utilities;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Map;

import static com.github.tommyettinger.digital.Hasher.*;
import static com.github.tommyettinger.ds.Utilities.neverIdentical;

/**
 * A custom variant on ObjectObjectOrderedMap that always uses String keys and compares them as case-insensitive.
 * This uses a fairly complex hashing function based on
 * <a href="https://github.com/wangyi-fudan/wyhash/blob/version_1/wyhash.h">wyhash by Wang Yi</a>.
 * It hashes without allocating new Strings all over, where many case-insensitive
 * algorithms do allocate quite a lot, but it does this by handling case incorrectly for the Georgian alphabet.
 * If I see Georgian text in-the-wild, I may reconsider, but I don't think that particular alphabet is in
 * widespread use. This uses {@link String#equalsIgnoreCase(String)} to compare keys for equality.
 */
public class CaselessOrderedMap<V> extends ObjectObjectOrderedMap<String, V> {

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public CaselessOrderedMap() {
		super();
	}

	/**
	 * Creates a new map with the specified initial capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This map will hold initialCapacity items before growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public CaselessOrderedMap(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public CaselessOrderedMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map an ObjectObjectOrderedMap to copy, or a subclass such as this one
	 */
	public CaselessOrderedMap(ObjectObjectOrderedMap<? extends String, ? extends V> map) {
		super(map);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map a Map to copy; ObjectObjectOrderedMap and subclasses of it will be faster
	 */
	public CaselessOrderedMap(Map<? extends String, ? extends V> map) {
		super(map);
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public CaselessOrderedMap(String[] keys, V[] values) {
		super(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 */
	public CaselessOrderedMap(Collection<? extends String> keys, Collection<? extends V> values) {
		super(keys, values);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given ObjectObjectOrderedMap (or a subclass, such as
	 * CaselessOrderedMap), starting at {@code offset} in that Map, into this.
	 *
	 * @param other  another ObjectObjectOrderedMap of the same types (key must be String)
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public CaselessOrderedMap(ObjectObjectOrderedMap<String, ? extends V> other, int offset, int count) {
		this(count);
		putAll(0, other, offset, count);
	}

	public static int hashCaseless(long seed, final String data) {
		if (data == null)
			return 0;
		final int len = data.length();
		for (int i = 3; i < len; i += 4) {
			seed = mum(
					mum(Character.toUpperCase(data.charAt(i - 3)) ^ b1, Character.toUpperCase(data.charAt(i - 2)) ^ b2) - seed,
					mum(Character.toUpperCase(data.charAt(i - 1)) ^ b3, Character.toUpperCase(data.charAt(i)) ^ b4));
		}
		switch (len & 3) {
			case 0:
				seed = mum(b1 - seed, b4 + seed);
				break;
			case 1:
				seed = mum(b5 - seed, b3 ^ Character.toUpperCase(data.charAt(len - 1)));
				break;
			case 2:
				seed = mum(Character.toUpperCase(data.charAt(len - 2)) - seed, b0 ^ Character.toUpperCase(data.charAt(len - 1)));
				break;
			case 3:
				seed = mum(Character.toUpperCase(data.charAt(len - 3)) - seed, b2 ^ Character.toUpperCase(data.charAt(len - 2))) + mum(b5 ^ seed, b4 ^ Character.toUpperCase(data.charAt(len - 1)));
				break;
		}
		seed = (seed ^ len) * (seed << 16 ^ b0);
		return (int)(seed ^ (seed << 33 | seed >>> 31) ^ (seed << 19 | seed >>> 45));

	}

	@Override
	protected int place (Object item) {
		if (item instanceof String)
			return hashCaseless(hashMultiplier, (String)item) & mask;
		return super.place(item);
	}

	@Override
	protected boolean equate (Object left, @Nullable Object right) {
		if ((left instanceof String) && (right instanceof String)) {
			return ((String)left).equalsIgnoreCase((String)right);
		}
		return false;
	}

	@Override
	public int hashCode () {
		int h = size;
		String[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			String key = keyTable[i];
			if (key != null) {
				h ^= hashCaseless(9069147967908697017L, key);
				V value = valueTable[i];
				if (value != null) {h ^= value.hashCode();}
			}
		}
		return h;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public boolean equals (Object obj) {
		if (obj == this) {return true;}
		if (!(obj instanceof CaselessOrderedMap)) {return false;}
		CaselessOrderedMap other = (CaselessOrderedMap)obj;
		if (other.size != size) {return false;}
		Object[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			Object key = keyTable[i];
			if (key != null) {
				V value = valueTable[i];
				if (value == null) {
					if (other.getOrDefault(key, neverIdentical) != null) {return false;}
				} else {
					if (!value.equals(other.get(key))) {return false;}
				}
			}
		}
		return true;
	}

	@Override
	public ObjectObjectMap.Keys<String, V> keySet () {
		if (keys1 == null || keys2 == null) {
			keys1 = new Keys<>(this);
			keys2 = new Keys<>(this);
		}
		if (!keys1.iterator().valid) {
			keys1.iterator().reset();
			keys1.iterator().valid = true;
			keys2.iterator().valid = false;
			return keys1;
		}
		keys2.iterator().reset();
		keys2.iterator().valid = true;
		keys1.iterator().valid = false;
		return keys2;
	}

	public static class Keys<V> extends OrderedMapKeys<String, V> {

		public Keys (ObjectObjectOrderedMap<String, V> map) {
			super(map);
		}

		@Override
		public int hashCode () {
			int h = 0;
			iter.reset();
			while (iter.hasNext()) {
				String obj = iter.next();
				if (obj != null)
					h += hashCaseless(9069147967908697017L, obj);
			}
			return h;
		}
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   the first and only key
	 * @param value0 the first and only value
	 * @param <V>    the type of value0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <V> CaselessOrderedMap<V> with (String key0, V value0) {
		CaselessOrderedMap<V> map = new CaselessOrderedMap<>(1);
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #CaselessOrderedMap(String[], Object[])}, which takes all keys and then all values.
	 * This needs all keys to be {@code String}s (like String or StringBuilder) and all values to
	 * have the same type, because it gets those types from the first value parameter. Any keys that
	 * aren't Strings or values that don't have V as their type have that entry skipped.
	 *
	 * @param key0   the first key; will be used to determine the type of all keys
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   an array or varargs of alternating K, V, K, V... elements
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	@SuppressWarnings("unchecked")
	public static <V> CaselessOrderedMap<V> with (String key0, V value0, Object... rest) {
		CaselessOrderedMap<V> map = new CaselessOrderedMap<>(1 + (rest.length >>> 1));
		map.put(key0, value0);
		for (int i = 1; i < rest.length; i += 2) {
			try {
				map.put((String)rest[i - 1], (V)rest[i]);
			} catch (ClassCastException ignored) {
			}
		}
		return map;
	}
}
