package com.jcraft.jsch;

class RetrospectiveRequestSftp extends Request {

  RetrospectiveRequestSftp() {
    setReply(true);
  }

  public void sendRequest(Session session, Channel channel) throws Exception {
    new SftpRequestSender().request(session, channel);
  }

  public void sendRequestWithSudo(Session session, Channel channel, String sudoCommand)
      throws Exception {
    new SudoedSftpRequestSender(sudoCommand).request(session, channel);
  }

  private class SftpRequestSender {

    /**
     * This method was copied from RequestSftp class and then adapted to provide custom
     * {@code channelRequestType} and {@code command}.
     */
    void request(Session session, Channel channel) throws Exception {
      RetrospectiveRequestSftp.super.request(session, channel);
      Buffer buf = new Buffer();
      Packet packet = new Packet(buf);
      packet.reset();
      buf.putByte((byte) Session.SSH_MSG_CHANNEL_REQUEST);
      buf.putInt(channel.getRecipient());
      buf.putString(Util.str2byte(getChannelRequestType()));
      buf.putByte((byte) (waitForReply() ? 1 : 0));
      buf.putString(Util.str2byte(getCommand()));
      write(packet);
    }

    String getChannelRequestType() {
      return "subsystem";
    }

    String getCommand() {
      return "sftp";
    }
  }

  private class SudoedSftpRequestSender extends SftpRequestSender {

    private final String sudoCommand;

    private SudoedSftpRequestSender(String sudoCommand) {
      this.sudoCommand = sudoCommand;
    }

    @Override
    String getChannelRequestType() {
      return "exec";
    }

    @Override
    String getCommand() {
      return sudoCommand;
    }
  }
}
