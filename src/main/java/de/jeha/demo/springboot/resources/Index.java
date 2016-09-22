package de.jeha.demo.springboot.resources;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author jenshadlich@googlemail.com
 */
@RestController
public class Index {

    @RequestMapping("/")
    protected void redirectToBrowse(HttpServletResponse response) throws IOException {
        response.sendRedirect("/browse");
    }

}
