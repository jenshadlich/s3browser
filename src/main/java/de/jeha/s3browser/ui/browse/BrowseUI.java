package de.jeha.s3browser.ui.browse;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.vaadin.annotations.Theme;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.Page;
import com.vaadin.server.StreamResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import de.jeha.s3browser.model.DetailsEntry;
import de.jeha.s3browser.model.ListEntry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author jenshadlich@googlemail.com
 */
@SpringUI(path = "browse")
@Theme("valo")
public class BrowseUI extends UI {

    private static final Logger LOG = LoggerFactory.getLogger(BrowseUI.class);

    private final Grid grid;
    private final Panel details;

    private TextField accessKey = new TextField("accessKey");
    private TextField secretKey = new TextField("secretKey");
    private TextField endpoint = new TextField("endpoint");
    private TextField bucket = new TextField("bucket");
    private Button connectButton = new Button("Connect");

    private AmazonS3 s3Client;
    private String prefix;

    public BrowseUI() {
        grid = new Grid();
        details = new Panel();
    }

    @Override
    protected void init(VaadinRequest request) {
        final HorizontalLayout main = new HorizontalLayout();

        final VerticalLayout menu = new VerticalLayout();
        menu.setSpacing(true);

        connectButton.addClickListener(e -> {
            connectToS3(accessKey.getValue(), secretKey.getValue(), endpoint.getValue());
            updateList();
            prefix = null;
        });

        accessKey.setWidth(300, Unit.PIXELS);
        secretKey.setWidth(300, Unit.PIXELS);
        endpoint.setWidth(200, Unit.PIXELS);
        bucket.setWidth(200, Unit.PIXELS);

        accessKey.setStyleName(ValoTheme.TEXTFIELD_TINY);
        secretKey.setStyleName(ValoTheme.TEXTFIELD_TINY);
        endpoint.setStyleName(ValoTheme.TEXTFIELD_TINY);
        bucket.setStyleName(ValoTheme.TEXTFIELD_TINY);

        menu.addComponent(accessKey);
        menu.addComponent(secretKey);
        menu.addComponent(endpoint);
        menu.addComponent(bucket);
        menu.addComponent(connectButton);
        menu.setWidth(330, Unit.PIXELS);

        details.setWidth(400, Unit.PIXELS);
        details.setContent(new Label("DETAILS"));
        details.setVisible(false);

        main.addComponent(menu);
        main.addComponent(grid);
        main.addComponent(details);
        main.setExpandRatio(grid, 1.0f);
        main.setMargin(true);
        main.setSpacing(true);
        main.setSizeFull();

        updateList();

        grid.setColumns("key");
        grid.setSizeFull();

        grid.addSelectionListener(l -> {
            Set selected = l.getSelected();
            if (selected.size() == 1) {
                ListEntry item = (ListEntry) selected.toArray()[0];
                LOG.info("Selected '{}'", item.getKey());
                if (!item.isObject()) {
                    if ("..".equals(item.getKey())) {
                        if (StringUtils.countMatches(item.getPrefix(), "/") > 1) {
                            prefix = StringUtils.substringBeforeLast(StringUtils.removeEnd(prefix, "/"), "/") + "/";
                        } else {
                            prefix = null;
                        }
                    } else {
                        prefix = item.getKey();
                    }
                    updateList();
                    details.setVisible(false);
                } else {
                    updateDetails(item);
                    details.setVisible(true);
                }
            }
        });

        Page.getCurrent().setTitle("Browse");
        setContent(main);
    }

    private void updateList() {
        final String bucket = this.bucket.getValue();
        LOG.info("List bucket '{}'", bucket);

        List<ListEntry> objects = new ArrayList<>();
        if (s3Client != null) {
            ObjectListing objectListing = s3Client.listObjects(new ListObjectsRequest(bucket, prefix, null, "/", null));
            if (prefix != null) {
                objects.add(new ListEntry(bucket, prefix, "..", false));
            }

            LOG.info("ObjectListing#commonPrefixes.size()={}", objectListing.getCommonPrefixes().size());
            for (String commonPrefix : objectListing.getCommonPrefixes()) {
                objects.add(new ListEntry(bucket, prefix, commonPrefix, false));
            }

            LOG.info("ObjectListing#objectSummaries.size()={}", objectListing.getObjectSummaries().size());
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                objects.add(new ListEntry(bucket, prefix, objectSummary.getKey(), true));
            }
        }

        BeanItemContainer<ListEntry> container = new BeanItemContainer<>(ListEntry.class, objects);
        grid.setContainerDataSource(container);
    }

    private void updateDetails(ListEntry item) {
        LOG.info("Display details for {}", item);

        ObjectMetadata objectMetadata = s3Client.getObjectMetadata(item.getBucket(), item.getKey());

        VerticalLayout detailsContent = new VerticalLayout();
        detailsContent.setSpacing(true);
        detailsContent.setMargin(true);
        Label caption = new Label(item.getKey());
        caption.addStyleName(ValoTheme.LABEL_H3);
        detailsContent.addComponent(caption);

        Grid detailsGrid = new Grid();
        List<DetailsEntry> detailsEntries = new ArrayList<>();

        detailsEntries.add(new DetailsEntry("LastModified", objectMetadata.getLastModified().toString()));
        detailsEntries.add(new DetailsEntry("ContentType", objectMetadata.getContentType()));
        detailsEntries.add(new DetailsEntry("ContentLength", Long.toString(objectMetadata.getContentLength())));

        detailsGrid.setContainerDataSource(new BeanItemContainer<>(DetailsEntry.class, detailsEntries));
        //detailsContent.addComponent(detailsGrid);

        detailsContent.addComponent(buildDetailsTextField("LastModified", objectMetadata.getLastModified().toString()));
        detailsContent.addComponent(buildDetailsTextField("ContentLength", Long.toString(objectMetadata.getContentLength())));
        detailsContent.addComponent(buildDetailsTextField("ContentType", objectMetadata.getContentType()));
        detailsContent.addComponent(buildDetailsTextField("ETag", objectMetadata.getETag()));

        Button downloadButton = new Button("Download");

        FileDownloader fileDownloader = new FileDownloader(new StreamResource(() -> {
            LOG.info("Download {}/{}", item.getBucket(), item.getKey());
            return s3Client.getObject(item.getBucket(), item.getKey()).getObjectContent();
        }, item.getKey()));
        fileDownloader.extend(downloadButton);

        detailsContent.addComponent(downloadButton);

        details.setContent(detailsContent);
    }

    private TextField buildDetailsTextField(String caption, String value) {
        TextField textField = new TextField(caption, value);
        textField.setStyleName(ValoTheme.TEXTFIELD_TINY);
        textField.setEnabled(false);
        textField.setWidth(300, Unit.PIXELS);
        return textField;
    }

    private void connectToS3(String accessKey, String secretKey, String endpoint) {
        LOG.info("'{}','{}','{}'", accessKey, secretKey, endpoint);
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        ClientConfiguration clientConfiguration = new ClientConfiguration()
                .withProtocol(Protocol.HTTP)
                .withUserAgentPrefix("s3browser")
                .withSignerOverride("S3SignerType");

        s3Client = new AmazonS3Client(credentials, clientConfiguration);
        s3Client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).disableChunkedEncoding().build());
        s3Client.setEndpoint(endpoint);
    }

}