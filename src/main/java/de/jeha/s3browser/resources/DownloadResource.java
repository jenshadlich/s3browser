package de.jeha.s3browser.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author jenshadlich@googlemail.com
 */
@RestController
public class DownloadResource {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadResource.class);

    @RequestMapping(value = "/download/{token}", method = RequestMethod.GET)
    public void download(@PathVariable("token") String token, HttpServletResponse response) throws IOException {
        LOG.info("Download token='{}'", token);
        //InputStream is = null;
        //org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
        //response.flushBuffer();
    }

}
