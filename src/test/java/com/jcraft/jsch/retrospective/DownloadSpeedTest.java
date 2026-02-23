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
    String command = "cat /home/retroit/integration/fetch/30M.log";
    ChannelExec channel = executeCommand(command);
    InputStream inputStream = channel.getInputStream();
    System.out.println("InputStream class: " + inputStream.getClass().getName());
    channel.connect();
    ((RetrospectiveChannelExec) channel).execute();
    // BufferedInputStream disabled to match old test exactly
    // inputStream = new java.io.BufferedInputStream(inputStream, 256 * 1024);
    byte[] buff = new byte[32 * 1024];
    int total = 0;
    int count = 0;
    StopWatch watch = new StopWatch();
    watch.start();
    int read = 0, not = 0;
    long availableTime = 0, readTime = 0;
    long minRead = Long.MAX_VALUE, maxRead = 0;
    // Use blocking read instead of polling pattern
    while (true) {
      long t2 = System.nanoTime();
      count = inputStream.read(buff);
      long elapsed = System.nanoTime() - t2;
      readTime += elapsed;
      if (count > 0) {
        minRead = Math.min(minRead, elapsed);
        maxRead = Math.max(maxRead, elapsed);
      }
      if (count == -1) {
        break;
      }
      total += count;
      read++;
      // Only print first and last few reads to avoid spam
      if (read <= 5 || read >= 1860) {
        System.out.println(count + " --> " + total);
      }
    }
    System.out.println("Min read time: " + (minRead / 1000) + "us, Max read time: "
        + (maxRead / 1_000_000) + "ms");
    watch.stop();
    System.out.println(read + "\t" + not + "\t" + watch.getTime() + "ms");
    System.out.println("available() time: " + (availableTime / 1_000_000) + "ms, read() time: "
        + (readTime / 1_000_000) + "ms");
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
