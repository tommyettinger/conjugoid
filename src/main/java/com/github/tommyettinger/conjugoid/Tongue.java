package com.github.tommyettinger.conjugoid;

import com.github.tommyettinger.ds.ObjectObjectMap;
import com.github.tommyettinger.ds.ObjectObjectOrderedMap;
import com.github.tommyettinger.function.ObjToSameFunction;

import java.util.Locale;

/**
 * Represents a natural language or some related concept, like barking for dogs or meowing for cats.
 */
public class Tongue {
    public final Locale locale;

    public final ObjectObjectMap<String, Pronoun> pronouns = new ObjectObjectMap<>(16);

    public Tongue() {
        this(new Locale("dog")); // woof, woof! let's see if this works!
    }

    public Tongue(Locale locale) {
        this.locale = locale;
    }

    public Tongue registerPronoun(Pronoun p) {
        pronouns.put(p.tag, p);
        return this;
    }

    public static class Pronoun {
        public final ObjectObjectOrderedMap<String, ObjToSameFunction<String>> substitutions = new ObjectObjectOrderedMap<>(32);
        public final String tag;
        public Pronoun() {
            this("t3s"); // they/them, 3rd-person, singular
            substitutions.put("i", s -> "they");
            substitutions.put("me", s -> "them");
            substitutions.put("my", s -> "their");
            substitutions.put("mine", s -> "theirs");
            substitutions.put("myself", s -> "themself");
            substitutions.put("s", s -> "");
            substitutions.put("es", s -> "");
            substitutions.put("sss", s -> "y");
            substitutions.put("usi", s -> "us");
            substitutions.put("fves", s -> "f");
            substitutions.put("$", s -> "");
            substitutions.put("$$", s -> "");
            substitutions.put("$$$", s -> "y");
            substitutions.put("name", s -> s);
            substitutions.put("name_s", s -> s.isEmpty() ? "" : s.endsWith("s") ? s + '\'' : s + "'s");
            substitutions.put("direct", s -> s);
        }
        public Pronoun(String tag) {
            this.tag = tag;
        }
    }
}
