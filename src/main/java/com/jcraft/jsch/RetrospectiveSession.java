package com.jcraft.jsch;

public class RetrospectiveSession extends Session {

  RetrospectiveSession(JSch jsch, String username, String host, int port) throws JSchException {
    super(jsch, username, host, port);
  }

  @Override
  protected Channel instantiateChannel(String type) {
    if ("exec".equals(type)) {
      return new RetrospectiveChannelExec();
    }
    if ("sftp".equals(type)) {
      return new RetrospectiveChannelSftp();
    }
    return super.instantiateChannel(type);
  }
}
