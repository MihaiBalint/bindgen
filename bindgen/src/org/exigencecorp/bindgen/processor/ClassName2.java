package org.exigencecorp.bindgen.processor;

public class ClassName2 {

    private final String fullClassNameWithGenerics;

    public ClassName2(String fullClassNameWithGenerics) {
        this.fullClassNameWithGenerics = fullClassNameWithGenerics;
    }

    public String getWithoutGenericPart() {
        int firstBracket = this.fullClassNameWithGenerics.indexOf("<");
        if (firstBracket != -1) {
            return this.fullClassNameWithGenerics.substring(0, firstBracket);
        }
        return this.fullClassNameWithGenerics;
    }

    /** @return "Type" if the type is "com.app.Type<String, String>" */
    public String getSimpleName() {
        String p = this.getWithoutGenericPart();
        int lastDot = p.lastIndexOf('.');
        if (lastDot == -1) {
            return p;
        } else {
            return p.substring(lastDot + 1);
        }
    }

    /** @return "com.app" if the type is "com.app.Type<String, String>" */
    public String getPackageName() {
        String p = this.getWithoutGenericPart();
        int lastDot = p.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        } else {
            return p.substring(0, lastDot);
        }
    }

}