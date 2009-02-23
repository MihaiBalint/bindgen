package org.exigencecorp.bindgen.example.methods;

import org.exigencecorp.bindgen.Bindable;

@Bindable
public class MethodExample {

    // a read-only property
    private String id;
    // a read/write property
    private String name;
    // a boolean property
    private boolean good;

    public MethodExample(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return "method";
    }

    public boolean isGood() {
        return this.good;
    }

    public void setGood(boolean good) {
        this.good = good;
    }

    public boolean hasStuff() {
        return false;
    }

    // Putting the @deprecated here ensures a warning would show up if is "to" prefix got recognized
    @Deprecated
    public boolean tobacco() {
        return false;
    }

    // This method would be a property "new" which is a keyword
    public boolean isNew() {
        return false;
    }
}
