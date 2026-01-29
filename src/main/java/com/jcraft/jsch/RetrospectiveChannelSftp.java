package com.jcraft.jsch;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class RetrospectiveChannelSftp extends ChannelSftp {

  private String sudoCommand;
  private String sudoPasswordEndedWithNewLine;

  public RetrospectiveChannelSftp() {
    super();
  }

  public void connectWithSudo(int connectTimeout, String sudoCommand, String sudoPassword)
      throws JSchException {
    this.sudoPasswordEndedWithNewLine = sudoPassword + "\n";
    connectWithSudo(connectTimeout, sudoCommand);
  }

  public void connectWithSudo(int connectTimeout, String sudoCommand) throws JSchException {
    this.sudoCommand = sudoCommand;
    super.connect(connectTimeout);
  }

  void sendRequestSftp() throws JSchException, Exception {
    RetrospectiveRequestSftp request = new RetrospectiveRequestSftp();
    if (sudoCommand == null) {
      sendReqularRequestSftp(request);
    } else {
      sendSudoedRequestSftp(request);
    }
  }

  private void sendReqularRequestSftp(RetrospectiveRequestSftp request)
      throws JSchException, Exception {
    request.sendRequest(getSession(), this);
  }

  private void sendSudoedRequestSftp(RetrospectiveRequestSftp request)
      throws JSchException, Exception {
    request.sendRequestWithSudo(getSession(), this, sudoCommand);
    providePasswordIfNeeded();
  }

  private void providePasswordIfNeeded() throws IOException {
    if (sudoPasswordEndedWithNewLine != null) {
      OutputStream out = getOutputStream();
      out.write(sudoPasswordEndedWithNewLine.getBytes(StandardCharsets.UTF_8));
      out.flush();
    }
  }
}
