package de.jeha.demo.springboot.model;

/**
 * @author jenshadlich@googlemail.com
 */
public class ListEntry {

    private final String bucket;
    private final String prefix;
    private final String key;
    private final boolean isObject;

    public ListEntry(String bucket, String prefix, String key, boolean isObject) {
        this.bucket = bucket;
        this.prefix = prefix;
        this.key = key;
        this.isObject = isObject;
    }

    public String getBucket() {
        return bucket;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getKey() {
        return key;
    }

    public boolean isObject() {
        return isObject;
    }

}
