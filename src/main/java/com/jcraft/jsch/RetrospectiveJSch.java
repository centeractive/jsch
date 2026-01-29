package com.jcraft.jsch;

public class RetrospectiveJSch extends JSch {

  @Override
  public Session getSession(String username, String host) throws JSchException {
    return getSession(username, host, 22);
  }

  @Override
  public Session getSession(String username, String host, int port) throws JSchException {
    if (username == null) {
      throw new JSchException("username must not be null.");
    }
    if (host == null) {
      throw new JSchException("host must not be null.");
    }
    return new RetrospectiveSession(this, username, host, port);
  }
}
