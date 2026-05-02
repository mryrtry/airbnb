package org.mryrt.airbnb.auth.jaas;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.mryrt.airbnb.auth.model.Role;
import org.mryrt.airbnb.auth.model.User;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class XmlCredentialsStore {

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Value("${airbnb.credentials.xml.path}")
    private String xmlFilePath;

    private final AtomicLong idSequence = new AtomicLong(1);

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
        } else {
            long maxId = findAll().stream()
                    .mapToLong(u -> u.getId() != null ? u.getId() : 0L)
                    .max()
                    .orElse(0L);
            idSequence.set(maxId + 1);
        }
    }

    public String getFilePath() {
        return new File(xmlFilePath).getAbsolutePath();
    }

    public synchronized Optional<User> findByUsername(String username) {
        return findAll().stream()
                .filter(u -> username.equals(u.getUsername()))
                .findFirst();
    }

    public synchronized Optional<User> findById(Long id) {
        return findAll().stream()
                .filter(u -> id.equals(u.getId()))
                .findFirst();
    }

    public synchronized boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    public synchronized List<User> findAll() {
        try {
            File file = new File(xmlFilePath);
            Document doc = load(file);
            NodeList userNodes = doc.getElementsByTagName("user");
            List<User> users = new ArrayList<>();
            for (int i = 0; i < userNodes.getLength(); i++) {
                users.add(elementToUser((Element) userNodes.item(i)));
            }
            return users;
        } catch (Exception e) {
            log.error("Failed to read users from credentials file: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public synchronized void saveUser(User user) {
        try {
            File file = new File(xmlFilePath);
            Document doc = load(file);
            Element usersEl = doc.getDocumentElement();

            boolean isNew = user.getId() == null;
            if (isNew) {
                user.setId(idSequence.getAndIncrement());
                user.setCreatedAt(LocalDateTime.now());
            } else {
                // preserve createdAt from existing entry
                findById(user.getId()).ifPresent(existing -> {
                    if (user.getCreatedAt() == null) user.setCreatedAt(existing.getCreatedAt());
                });
            }
            user.setUpdatedAt(LocalDateTime.now());

            removeUserElement(doc, user.getUsername());

            Element userEl = userToElement(doc, user);
            usersEl.appendChild(userEl);
            save(doc, file);
        } catch (Exception e) {
            log.error("Failed to save user '{}' in credentials file: {}", user.getUsername(), e.getMessage());
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

    private User elementToUser(Element el) {
        User user = new User();
        String idStr = el.getAttribute("id");
        if (idStr != null && !idStr.isBlank()) {
            user.setId(Long.parseLong(idStr));
        }
        user.setUsername(el.getAttribute("username"));
        user.setEmail(el.getAttribute("email"));
        user.setPassword(el.getAttribute("password"));

        String createdAt = el.getAttribute("createdAt");
        if (createdAt != null && !createdAt.isBlank()) {
            user.setCreatedAt(LocalDateTime.parse(createdAt, DT_FORMAT));
        }
        String updatedAt = el.getAttribute("updatedAt");
        if (updatedAt != null && !updatedAt.isBlank()) {
            user.setUpdatedAt(LocalDateTime.parse(updatedAt, DT_FORMAT));
        }

        Set<Role> roles = new HashSet<>();
        NodeList roleNodes = el.getElementsByTagName("role");
        for (int i = 0; i < roleNodes.getLength(); i++) {
            String roleName = roleNodes.item(i).getTextContent().trim();
            try {
                roles.add(Role.valueOf(roleName));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown role '{}' for user '{}'", roleName, user.getUsername());
            }
        }
        user.setRoles(roles);
        return user;
    }

    private Element userToElement(Document doc, User user) {
        Element userEl = doc.createElement("user");
        userEl.setAttribute("id", String.valueOf(user.getId()));
        userEl.setAttribute("username", user.getUsername());
        userEl.setAttribute("email", user.getEmail() != null ? user.getEmail() : "");
        userEl.setAttribute("password", user.getPassword());
        if (user.getCreatedAt() != null) {
            userEl.setAttribute("createdAt", user.getCreatedAt().format(DT_FORMAT));
        }
        if (user.getUpdatedAt() != null) {
            userEl.setAttribute("updatedAt", user.getUpdatedAt().format(DT_FORMAT));
        }
        for (Role role : user.getRoles()) {
            Element roleEl = doc.createElement("role");
            roleEl.setTextContent(role.name());
            userEl.appendChild(roleEl);
        }
        return userEl;
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
