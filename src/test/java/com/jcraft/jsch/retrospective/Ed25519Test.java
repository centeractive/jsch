package com.jcraft.jsch.retrospective;

import com.jcraft.jsch.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class Ed25519Test {

  private static final String KEY_RSA =
      Ed25519Test.class.getResource("/keys/key1_id_rsa").getFile();
  private static final String KEY_ED25519 =
      Ed25519Test.class.getResource("/keys/key3_id_ed25519").getFile();
  private static final String USER = "<R>";
  private static final String KEYPASS_CORRECT = "<R>";
  private static final String KEYPASS_WRONG = "wrongKeyPass";
  private static final int CONNECT_TIMEOUT = 30_000;

  @Test
  public void testEd25519Auth() throws JSchException, SftpException {
    JschSessionWrapper sessionWrapper = new JschSessionWrapper(USER, KEY_ED25519, KEYPASS_CORRECT);
    Session session = sessionWrapper.open(CONNECT_TIMEOUT);
    boolean found = findFileThroughSftp(session);
    session.disconnect();

    assertTrue(found);
  }

  @Test
  public void testEd25519Auth_wrongKeyPass() throws Throwable {
    JschSessionWrapper sessionWrapper = new JschSessionWrapper(USER, KEY_ED25519, KEYPASS_WRONG);
    RuntimeException runtimeException =
        assertThrows(RuntimeException.class, () -> sessionWrapper.open(CONNECT_TIMEOUT));

    // Jsch 0.1.55.2
    // assertEquals("Key cannot be decrypted because wrong keypass was provided",
    // ioException.getMessage());
    assertEquals("Cannot open SSH connection", runtimeException.getMessage());
    assertInstanceOf(JSchException.class, runtimeException.getCause());
    assertEquals("USERAUTH fail", runtimeException.getCause().getMessage());
  }

  @Test
  public void testEd25519Auth_nullKeyPass() {
    JschSessionWrapper sessionWrapper = new JschSessionWrapper(USER, KEY_ED25519, null);
    RuntimeException runtimeException =
        assertThrows(RuntimeException.class, () -> sessionWrapper.open(CONNECT_TIMEOUT));

    // Jsch 0.1.55.2
    // IllegalArgumentException illegalArgumentException =
    // assertThrows(IllegalArgumentException.class, () -> sessionWrapper.open(CONNECT_TIMEOUT));
    assertEquals("Cannot open SSH connection", runtimeException.getMessage());
    assertInstanceOf(NullPointerException.class, runtimeException.getCause());
    assertEquals(
        "Cannot invoke \"String.getBytes(java.nio.charset.Charset)\" because \"this.keyPass\" is null",
        runtimeException.getCause().getMessage());
  }

  @Test
  public void testEd25519Auth_emptyKeyPass() {
    JschSessionWrapper sessionWrapper = new JschSessionWrapper(USER, KEY_ED25519, "");
    RuntimeException runtimeException =
        assertThrows(RuntimeException.class, () -> sessionWrapper.open(CONNECT_TIMEOUT));

    // Jsch 0.1.55.2
    // assertEquals("Key is encrypted and no keypass was provided",
    // illegalArgumentException.getMessage());
    assertEquals("Cannot open SSH connection", runtimeException.getMessage());
    assertInstanceOf(JSchException.class, runtimeException.getCause());
    assertEquals("USERAUTH fail", runtimeException.getCause().getMessage());

  }

  @Test
  public void testRsaAuth() throws JSchException, SftpException {
    JschSessionWrapper sessionWrapper = new JschSessionWrapper(USER, KEY_RSA, KEYPASS_CORRECT);
    Session session = sessionWrapper.open(CONNECT_TIMEOUT);
    boolean found = findFileThroughSftp(session);
    session.disconnect();

    assertTrue(found);
  }

  private static boolean findFileThroughSftp(Session session) throws JSchException, SftpException {
    Channel channel = session.openChannel("sftp");
    channel.connect();
    ChannelSftp sftpChannel = (ChannelSftp) channel;
    boolean found = false;
    Vector<ChannelSftp.LsEntry> directoryEntries = sftpChannel.ls("/home/retro/.ssh");
    for (ChannelSftp.LsEntry file : directoryEntries) {
      System.out.println(file.getFilename());
      if (file.getFilename().contains("authorized_keys")) {
        found = true;
      }
    }
    sftpChannel.exit();
    return found;
  }
}
