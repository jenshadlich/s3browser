package de.jeha.demo.springboot.ui.browse;

import com.vaadin.annotations.Theme;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Grid;
import com.vaadin.ui.UI;
import de.jeha.demo.springboot.model.S3Bucket;
import de.jeha.demo.springboot.model.S3Object;

import java.util.Arrays;
import java.util.List;

/**
 * @author jenshadlich@googlemail.com
 */
@SpringUI(path = "browse")
@Theme("valo")
public class BrowseUI extends UI {

    private final Grid grid;

    public BrowseUI() {
        grid = new Grid();
    }

    @Override
    protected void init(VaadinRequest request) {
        setContent(grid);

        List<S3Object> objects = Arrays.asList(
                new S3Object(new S3Bucket("bucket"), "key1"),
                new S3Object(new S3Bucket("bucket"), "key2"));
        BeanItemContainer<S3Object> container = new BeanItemContainer<>(S3Object.class, objects);

        grid.setContainerDataSource(container);
        grid.removeColumn("bucket");

        Page.getCurrent().setTitle("Browse");
    }

}