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
import com.vaadin.annotations.Theme;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.Page;
import com.vaadin.server.StreamResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import de.jeha.s3browser.model.ListEntry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author jenshadlich@googlemail.com
 */
@SpringUI(path = "browse")
@Theme("valo")
public class BrowseUI extends UI {

    private static final Logger LOG = LoggerFactory.getLogger(BrowseUI.class);

    private final HorizontalLayout main;
    private final VerticalLayout left;
    private final Grid list;
    private final Panel details;

    private TextField accessKey = new TextField("Access Key");
    private PasswordField secretKey = new PasswordField("Secret Key");
    private TextField endpoint = new TextField("Endpoint");
    private TextField bucket = new TextField("Bucket");
    private CheckBox secure = new CheckBox("Use https");
    private CheckBox pathStyle = new CheckBox("Path-style access");
    private Button connect = new Button("Connect");

    private TextField filter = new TextField("Filter key by prefix");
    private AmazonS3 s3Client;
    private String prefix;
    private boolean insertDots = true;

    public BrowseUI() {
        main = new HorizontalLayout();
        left = new VerticalLayout();
        list = new Grid();
        details = new Panel();
    }

    @Override
    protected void init(VaadinRequest request) {
        left.setSpacing(true);

        connect.addClickListener(e -> {
            connectToS3(accessKey.getValue(), secretKey.getValue(), endpoint.getValue(), secure.getValue(), pathStyle.getValue());
            filter.setValue("");
            prefix = null;
            details.setVisible(false);
            updateList();
        });

        if (request.getParameter("accessKey") != null) {
            accessKey.setValue(request.getParameter("accessKey"));
        }
        if (request.getParameter("secretKey") != null) {
            secretKey.setValue(request.getParameter("secretKey"));
        }
        if (request.getParameter("endpoint") != null) {
            endpoint.setValue(request.getParameter("endpoint"));
        }
        if (request.getParameter("bucket") != null) {
            bucket.setValue(request.getParameter("bucket"));
        }

        pathStyle.setValue(Boolean.TRUE);

        accessKey.setWidth(300, Unit.PIXELS);
        secretKey.setWidth(300, Unit.PIXELS);
        endpoint.setWidth(200, Unit.PIXELS);
        bucket.setWidth(200, Unit.PIXELS);

        accessKey.setStyleName(ValoTheme.TEXTFIELD_TINY);
        secretKey.setStyleName(ValoTheme.TEXTFIELD_TINY);
        endpoint.setStyleName(ValoTheme.TEXTFIELD_TINY);
        bucket.setStyleName(ValoTheme.TEXTFIELD_TINY);
        secure.setStyleName(ValoTheme.TEXTFIELD_TINY);
        pathStyle.setStyleName(ValoTheme.TEXTFIELD_TINY);

        left.addComponent(accessKey);
        left.addComponent(secretKey);
        left.addComponent(endpoint);
        left.addComponent(bucket);
        left.addComponent(secure);
        left.addComponent(pathStyle);
        left.addComponent(connect);
        left.setWidth(330, Unit.PIXELS);
        left.addComponent(new Label("<hr />", ContentMode.HTML));
        left.addComponent(filter);

        details.setWidth(400, Unit.PIXELS);
        details.setContent(new Label("DETAILS"));
        details.setVisible(false);

        main.addComponent(left);
        main.addComponent(list);
        main.addComponent(details);
        main.setExpandRatio(list, 1.0f);
        main.setMargin(true);
        main.setSpacing(true);
        main.setSizeFull();

        updateList();

        list.setColumns("key");
        list.setSizeFull();

        list.addSelectionListener(l -> {
            insertDots = true;
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
                    this.updateList();
                    details.setVisible(false);
                } else {
                    this.updateDetails(item);
                    details.setVisible(true);
                }
            }
        });

        filter.addTextChangeListener(l -> {
            insertDots = false;
            prefix = l.getText();
            this.updateList();
        });

        Page.getCurrent().setTitle("Browse");
        setContent(main);
    }

    private void updateList() {
        final String bucket = this.bucket.getValue();
        LOG.info("List bucket '{}'", bucket);

        List<ListEntry> objects = new ArrayList<>();
        if (s3Client != null) {
            ObjectListing objectListing = s3Client.listObjects(new ListObjectsRequest(bucket, prefix, null, "/", 1_000));
            if (prefix != null && insertDots) {
                objects.add(new ListEntry(bucket, prefix, "..", false));
            }

            LOG.info("ObjectListing#commonPrefixes.size()={}", objectListing.getCommonPrefixes().size());
            objects.addAll(objectListing.getCommonPrefixes()
                    .stream()
                    .map(commonPrefix -> new ListEntry(bucket, prefix, commonPrefix, false))
                    .collect(Collectors.toList()));

            LOG.info("ObjectListing#objectSummaries.size()={}", objectListing.getObjectSummaries().size());
            objects.addAll(objectListing.getObjectSummaries()
                    .stream()
                    .map(objectSummary -> new ListEntry(bucket, prefix, objectSummary.getKey(), true))
                    .collect(Collectors.toList()));
        }

        BeanItemContainer<ListEntry> container = new BeanItemContainer<>(ListEntry.class, objects);
        list.setContainerDataSource(container);
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

        if (objectMetadata.getLastModified() != null) {
            detailsContent.addComponent(buildDetailsTextField("LastModified", objectMetadata.getLastModified().toString()));
        }
        detailsContent.addComponent(buildDetailsTextField("ContentLength", Long.toString(objectMetadata.getContentLength())));
        if (objectMetadata.getContentType() != null) {
            detailsContent.addComponent(buildDetailsTextField("ContentType", objectMetadata.getContentType()));
        }
        if (objectMetadata.getETag() != null) {
            detailsContent.addComponent(buildDetailsTextField("ETag", objectMetadata.getETag()));
        }

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

    private void connectToS3(String accessKey, String secretKey, String endpoint, boolean secure, boolean pathStyle) {
        LOG.info("'{}','{}','{}'", accessKey, secretKey, endpoint);
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        ClientConfiguration clientConfiguration = new ClientConfiguration()
                .withProtocol(secure ? Protocol.HTTPS : Protocol.HTTP)
                .withUserAgentPrefix("s3browser")
                .withSignerOverride("S3SignerType");

        s3Client = new AmazonS3Client(credentials, clientConfiguration);
        s3Client.setS3ClientOptions(S3ClientOptions.builder()
                .setPathStyleAccess(pathStyle)
                .disableChunkedEncoding()
                .build());
        s3Client.setEndpoint(endpoint);
    }

}