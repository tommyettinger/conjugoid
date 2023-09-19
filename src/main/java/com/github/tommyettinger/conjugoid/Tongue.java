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
        this(new Locale("en__dog")); // woof, woof! let's see if this works!
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
        public Pronoun addSubstitutions(String... args){
            substitutions.ensureCapacity(substitutions.size() + (args.length >>> 1));
            for (int i = 1; i < args.length; i += 2) {
                try {
                    final String fs = args[i];
                    substitutions.put(args[i - 1], s -> fs);
                } catch (ClassCastException ignored) {
                }
            }
            return this;
        }
        public Pronoun() {
            this("t3s"); // they/them, 3rd-person, singular
            addSubstitutions("i", "they", "me", "them", "my", "their", "mine", "theirs", "myself", "themself",
                    "s", "", "es", "", "sss", "y", "usi", "us", "fves", "f",
                    "$", "", "$$", "", "$$$", "y");
            substitutions.put("name", s -> s);
            substitutions.put("name_s", s -> s.isEmpty() ? "" : s.endsWith("s") ? s + '\'' : s + "'s");
            substitutions.put("direct", s -> s);
        }
        public Pronoun(String tag) {
            this.tag = tag;
        }
    }
}
