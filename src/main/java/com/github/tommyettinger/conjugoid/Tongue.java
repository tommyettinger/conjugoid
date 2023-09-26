package com.github.tommyettinger.conjugoid;

import com.github.tommyettinger.ds.ObjectObjectMap;
import com.github.tommyettinger.ds.ObjectObjectOrderedMap;
import com.github.tommyettinger.function.ObjToSameFunction;

import java.util.Locale;

/**
 * Represents a natural language or some related concept, like barking for dogs or meowing for cats.
 * <br>
 * Example syntax playground:
 * <br>
 * The goblin@1s slash@1$$ @2me with @1my wicked blade@1s!
 * The goblin slashes you with her wicked blade!
 * The goblins slash you with their wicked blades!
 */
public class Tongue {
    public final Locale locale;
    public final String tag;

    public final ObjectObjectMap<String, Pronoun> pronouns = new ObjectObjectMap<>(16);

    public Tongue() {
        this(new Locale("en__puppy")); // woof, woof! let's see if this works!
    }

    public Tongue(Locale locale) {
        this.locale = locale;
        tag = locale.toLanguageTag();
    }

    public Tongue registerPronoun(Pronoun p) {
        pronouns.put(p.tag, p);
        return this;
    }

    public static final ObjectObjectOrderedMap<String, Tongue> REGISTRY = new ObjectObjectOrderedMap<>(16);

    static {
        Tongue us = new Tongue(Locale.US);
        us.registerPronoun(new Pronoun()); // t3s
        REGISTRY.put(us.tag, us);
    }

    public static class Pronoun {
        public final CaselessOrderedMap<ObjToSameFunction<String>> substitutions = new CaselessOrderedMap<>(32);
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
                    "s", "", "ss", "", "sss", "y", "usi", "us", "fves", "f",
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
