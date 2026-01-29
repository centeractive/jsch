package com.jcraft.jsch;

public class RetrospectiveChannelExec extends ChannelExec {

  RetrospectiveChannelExec() {
    super();
  }

  @Override
  public void start() throws JSchException {}

  public void execute() throws JSchException {
    super.start();
  }
}
