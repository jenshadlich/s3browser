package de.jeha.demo.springboot.resources;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * @author jenshadlich@googlemail.com
 */
@RestController
public class VersionResource {

    @RequestMapping(value = "/version", produces = "application/json")
    public String getVersion() throws IOException {
        return Resources.toString(Resources.getResource("version.json"), Charsets.UTF_8);
    }

}
