package org.mryrt.airbnb.auth.jaas;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.Set;

/**
 * Manages user credentials stored in the JAAS XML file.
 * Provides write operations that keep the file in sync with the application's user store.
 */
@Slf4j
@Component
public class XmlCredentialsStore {

    @Value("${airbnb.credentials.xml.path}")
    private String xmlFilePath;

    @PostConstruct
    public void initialize() {
        File file = new File(xmlFilePath);
        if (!file.exists()) {
            try {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                doc.appendChild(doc.createElement("users"));
                save(doc, file);
                log.info("Created credentials file at {}", file.getAbsolutePath());
            } catch (Exception e) {
                log.error("Failed to create credentials file at {}: {}", file.getAbsolutePath(), e.getMessage());
            }
        }
    }

    public String getFilePath() {
        return new File(xmlFilePath).getAbsolutePath();
    }

    public synchronized void addOrUpdateUser(String username, String encodedPassword, Set<String> roles) {
        try {
            File file = new File(xmlFilePath);
            Document doc = load(file);
            Element usersEl = doc.getDocumentElement();

            // Remove existing entry if present
            removeUserElement(doc, username);

            Element userEl = doc.createElement("user");
            userEl.setAttribute("username", username);
            userEl.setAttribute("password", encodedPassword);
            for (String role : roles) {
                Element roleEl = doc.createElement("role");
                roleEl.setTextContent(role);
                userEl.appendChild(roleEl);
            }
            usersEl.appendChild(userEl);
            save(doc, file);
        } catch (Exception e) {
            log.error("Failed to add/update user '{}' in credentials file: {}", username, e.getMessage());
        }
    }

    public synchronized void removeUser(String username) {
        try {
            File file = new File(xmlFilePath);
            Document doc = load(file);
            removeUserElement(doc, username);
            save(doc, file);
        } catch (Exception e) {
            log.error("Failed to remove user '{}' from credentials file: {}", username, e.getMessage());
        }
    }

    private void removeUserElement(Document doc, String username) {
        NodeList users = doc.getElementsByTagName("user");
        for (int i = 0; i < users.getLength(); i++) {
            Element el = (Element) users.item(i);
            if (username.equals(el.getAttribute("username"))) {
                el.getParentNode().removeChild(el);
                return;
            }
        }
    }

    private Document load(File file) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        if (file.exists() && file.length() > 0) {
            return builder.parse(file);
        }
        Document doc = builder.newDocument();
        doc.appendChild(doc.createElement("users"));
        return doc;
    }

    private void save(Document doc, File file) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.transform(new DOMSource(doc), new StreamResult(file));
    }
}
