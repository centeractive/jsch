package com.jcraft.jsch.retrospective;

import com.jcraft.jsch.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class KeysTest {

  class JschLogger implements Logger {

    public boolean isEnabled(int arg0) {
      return true;
    }

    public void log(int arg0, String arg1) {
      System.out.println(System.currentTimeMillis() + " [SFTP/SSH; " + arg1);
    }
  }

  @Test
  public void testConnectionKeysAmbiguity()
      throws JSchException, SftpException, FileNotFoundException, IOException {
    JSch jsch = new JSch();
    JSch.setLogger(new JschLogger());
    String host = "<DEN>";
    int port = 22;
    String user = "<R>";
    URL keyUrl1 = this.getClass().getResource("/keys/key1_id_rsa");
    jsch.addIdentity(keyUrl1.getFile(), "retro");
    URL keyUrl2 = this.getClass().getResource("/keys/key2_id_rsa");
    jsch.addIdentity(keyUrl2.getFile(), "retro");
    Session session = jsch.getSession(user, host, port);
    session.setConfig("StrictHostKeyChecking", "no");
    session.connect();
    Channel channel = session.openChannel("sftp");
    channel.connect();
    ChannelSftp sftpChannel = (ChannelSftp) channel;
    boolean found = false;
    Vector<ChannelSftp.LsEntry> directoryEntries = sftpChannel.ls(".ssh");
    for (ChannelSftp.LsEntry file : directoryEntries) {
      System.out.println(file.getFilename());
      if (file.getFilename().contains("authorized_keys")) {
        found = true;
      }
    }
    sftpChannel.exit();
    session.disconnect();
    assertTrue(found);
  }


  @Test
  public void testRsaKeySize4096()
      throws JSchException, SftpException, FileNotFoundException, IOException {
    JSch jsch = new JSch();
    JSch.setLogger(new JschLogger());
    String host = "<DEN>";
    int port = 22;
    String user = "<R>";
    URL keyUrl = this.getClass().getResource("/keys/key4_id_rsa4096");
    jsch.addIdentity(keyUrl.getFile(), "retro");
    Session session = jsch.getSession(user, host, port);
    session.setConfig("StrictHostKeyChecking", "no");
    session.connect();
    Channel channel = session.openChannel("sftp");
    channel.connect();
    ChannelSftp sftpChannel = (ChannelSftp) channel;
    boolean found = false;
    Vector<ChannelSftp.LsEntry> directoryEntries = sftpChannel.ls(".ssh");
    for (ChannelSftp.LsEntry file : directoryEntries) {
      System.out.println(file.getFilename());
      if (file.getFilename().contains("authorized_keys")) {
        found = true;
      }
    }
    sftpChannel.exit();
    session.disconnect();
    assertTrue(found);
  }
}
