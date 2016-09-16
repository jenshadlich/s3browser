package de.jeha.demo.springboot.model;

/**
 * @author jenshadlich@googlemail.com
 */
public class S3Bucket {

    private String name;

    public S3Bucket(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
