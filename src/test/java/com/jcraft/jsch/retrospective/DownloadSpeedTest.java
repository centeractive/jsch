package com.jcraft.jsch.retrospective;

import com.jcraft.jsch.*;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

public class DownloadSpeedTest {

  @Disabled
  @Test
  public void execute_grep() throws Exception {
    String command = "cat /home/retroit/integration/logs/OX_BSCS-bscs1ox.log";
    ChannelExec channel = executeCommand(command);
    InputStream inputStream = channel.getInputStream();
    channel.connect();
    ((RetrospectiveChannelExec) channel).execute();
    // BufferedInputStream bin = new BufferedInputStream(in, 20000000);
    byte[] buff = new byte[1024 * 1024];
    int total = 0;
    int count = 0;
    StopWatch watch = new StopWatch();
    watch.start();
    int read = 0, not = 0;
    while (count != -1) {
      if (inputStream.available() > 0 || channel.isClosed()) {
        count = inputStream.read(buff);
        total += count;
        read++;
        System.out.println(count + " --> " + total);
      } else {
        not++;
        // log.info("not");
      }
      Thread.sleep(10);
    }
    watch.stop();
    System.out.println(read + "\t" + not + "\t" + watch.getTime() + "ms");
  }

  private ChannelExec executeCommand(String command) throws Exception {
    RetrospectiveJSch jsch = new RetrospectiveJSch();
    RetrospectiveSession session = (RetrospectiveSession) jsch.getSession("<R>", "<DEN>", 22);
    session.setPassword("<R>");
    session.setConfig("StrictHostKeyChecking", "no");
    session.setConfig("buffer_size", Integer.toString(1024 * 1024));
    session.connect();
    ChannelExec channel = (ChannelExec) session.openChannel("exec");
    channel.setCommand(command);
    channel.setInputStream(null);
    return channel;
  }
}
