package de.jeha.s3browser.model;

/**
 * @author jenshadlich@googlemail.com
 */
public class DetailsEntry {

    private final String key;
    private final String value;

    public DetailsEntry(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }


}
