package com.jcraft.jsch.retrospective;

import com.jcraft.jsch.*;

import java.nio.charset.StandardCharsets;

public class JschSessionWrapper {

  private static final String NO_PUBLIC_KEY = null;
  private static final int BROKEN_CONNECTION_CHECK_PERIOD_IN_MILLIS = 30000;
  private static final int SERVER_ALIVE_INTERVAL_IN_MILLIS = 30000;
  private final RetrospectiveJSch retrospectiveJSch;
  private Session session;
  private int connectTimeoutInMillis;
  private final String username;
  private final String keyPath;
  private final String keyPass;

  JschSessionWrapper(String username, String keyPath, String keyPass) {
    this.username = username;
    this.keyPath = keyPath;
    this.keyPass = keyPass;
    retrospectiveJSch = new RetrospectiveJSch();
  }

  Session open(int connectTimeoutInMillis) {
    try {
      this.connectTimeoutInMillis = connectTimeoutInMillis;
      session = openDataSourceSession();
      return session;
    } catch (Exception e) {
      throw new RuntimeException("Cannot open SSH connection", e);
    }
  }

  private Session openDataSourceSession() throws Exception {
    Session session = retrospectiveJSch.getSession(username, "<DEN>", 22);
    setSessionCredentials(session);
    setSessionParameters(session);
    connectSession(session);
    return session;
  }

  private void setSessionCredentials(Session session) throws JSchException {
    session.setIdentityRepository(generateIdentityRepository());
  }

  private LocalIdentityRepository generateIdentityRepository() throws JSchException {
    LocalIdentityRepository identityRepository =
        new LocalIdentityRepository(retrospectiveJSch.getJSchInstanceLogger());
    Identity key =
        IdentityFile.newInstance(keyPath, NO_PUBLIC_KEY, retrospectiveJSch.getJSchInstanceLogger());
    key.setPassphrase(keyPass.getBytes(StandardCharsets.UTF_8));
    identityRepository.add(key);
    return identityRepository;
  }

  private void setSessionParameters(Session session) throws Exception {
    session.setTimeout(BROKEN_CONNECTION_CHECK_PERIOD_IN_MILLIS);
    session.setServerAliveInterval(SERVER_ALIVE_INTERVAL_IN_MILLIS);
    session.setConfig("StrictHostKeyChecking", "no");
  }

  private void connectSession(Session session) throws Exception {
    session.connect(connectTimeoutInMillis);
    session.sendKeepAliveMsg();
  }
}
