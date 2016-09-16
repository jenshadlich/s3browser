package de.jeha.demo.springboot.model;

/**
 * @author jenshadlich@googlemail.com
 */
public class S3Object {

    private S3Bucket bucket;
    private String key;

    public S3Object(S3Bucket bucket, String key) {
        this.bucket = bucket;
        this.key = key;
    }

    public S3Bucket getBucket() {
        return bucket;
    }

    public void setBucket(S3Bucket bucket) {
        this.bucket = bucket;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
