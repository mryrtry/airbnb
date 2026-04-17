package org.mryrt.airbnb.auth.jaas;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JAAS LoginModule that authenticates users against an XML credentials file.
 * Passwords in the XML must be BCrypt-encoded.
 */
public class XmlFileLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> options;

    private String username;
    private boolean authenticated;

    private final List<UserPrincipal> userPrincipals = new ArrayList<>();
    private final List<RolePrincipal> rolePrincipals = new ArrayList<>();

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.options = options;
    }

    @Override
    public boolean login() throws LoginException {
        NameCallback nameCallback = new NameCallback("Username: ");
        PasswordCallback passwordCallback = new PasswordCallback("Password: ", false);
        try {
            callbackHandler.handle(new Callback[]{nameCallback, passwordCallback});
        } catch (Exception e) {
            throw new LoginException("Callback handling failed: " + e.getMessage());
        }

        username = nameCallback.getName();
        String password = new String(passwordCallback.getPassword());
        passwordCallback.clearPassword();

        authenticated = verifyCredentials(username, password);
        if (!authenticated) {
            throw new LoginException("Invalid username or password");
        }
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        if (!authenticated) return false;

        Document doc = loadDocument();
        NodeList userNodes = doc.getElementsByTagName("user");
        for (int i = 0; i < userNodes.getLength(); i++) {
            Element userEl = (Element) userNodes.item(i);
            if (username.equals(userEl.getAttribute("username"))) {
                userPrincipals.add(new UserPrincipal(username));
                NodeList roles = userEl.getElementsByTagName("role");
                for (int j = 0; j < roles.getLength(); j++) {
                    rolePrincipals.add(new RolePrincipal(roles.item(j).getTextContent().trim()));
                }
                break;
            }
        }

        subject.getPrincipals().addAll(userPrincipals);
        subject.getPrincipals().addAll(rolePrincipals);
        return true;
    }

    @Override
    public boolean abort() {
        cleanup();
        return true;
    }

    @Override
    public boolean logout() {
        subject.getPrincipals().removeAll(userPrincipals);
        subject.getPrincipals().removeAll(rolePrincipals);
        cleanup();
        return true;
    }

    private boolean verifyCredentials(String username, String password) {
        try {
            Document doc = loadDocument();
            NodeList userNodes = doc.getElementsByTagName("user");
            for (int i = 0; i < userNodes.getLength(); i++) {
                Element userEl = (Element) userNodes.item(i);
                if (username.equals(userEl.getAttribute("username"))) {
                    String encodedPassword = userEl.getAttribute("password");
                    return PASSWORD_ENCODER.matches(password, encodedPassword);
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private Document loadDocument() throws LoginException {
        try {
            String xmlFilePath = (String) options.get("xmlFilePath");
            InputStream is;

            if (xmlFilePath != null && !xmlFilePath.isBlank()) {
                File file = new File(xmlFilePath);
                if (file.exists()) {
                    is = file.toURI().toURL().openStream();
                } else {
                    // Fall back to classpath
                    is = Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream(xmlFilePath);
                }
            } else {
                is = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("users-credentials.xml");
            }

            if (is == null) {
                throw new LoginException("users-credentials.xml not found");
            }

            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw new LoginException("Failed to load credentials file: " + e.getMessage());
        }
    }

    private void cleanup() {
        userPrincipals.clear();
        rolePrincipals.clear();
        authenticated = false;
        username = null;
    }
}
