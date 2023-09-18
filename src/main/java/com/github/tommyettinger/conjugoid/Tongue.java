package com.github.tommyettinger.conjugoid;

import java.util.Locale;

/**
 * Represents a natural language or some related concept, like barking for dogs or meowing for cats.
 */
public class Tongue {
    public final Locale locale;

    public Tongue() {
        this(new Locale("dog")); // woof, woof! let's see if this works!
    }

    public Tongue(Locale locale) {
        this.locale = locale;
    }
}
