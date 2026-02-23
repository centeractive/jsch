package com.jcraft.jsch;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Custom PipedOutputStream implementation for better performance. This class is based on the
 * EnhancedPipedOutputStream from older Retrospective-JSch versions. It was copied to complement
 * RetrospectivePipedInputStream.
 */
class RetrospectivePipedOutputStream extends OutputStream {

  private RetrospectivePipedInputStream sink;

  /**
   * Creates a piped output stream connected to the specified piped input stream. Data bytes written
   * to this stream will then be available as input from <code>snk</code>.
   *
   * @param snk The piped input stream to connect to.
   * @exception IOException if an I/O error occurs.
   */
  public RetrospectivePipedOutputStream(RetrospectivePipedInputStream snk) throws IOException {
    connect(snk);
  }

  /**
   * Creates a piped output stream that is not yet connected to a piped input stream. It must be
   * connected to a piped input stream, either by the receiver or the sender, before being used.
   */
  public RetrospectivePipedOutputStream() {}

  /**
   * Connects this piped output stream to a receiver. If this object is already connected to some
   * other piped input stream, an <code>IOException</code> is thrown.
   *
   * @param snk the piped input stream to connect to.
   * @exception IOException if an I/O error occurs.
   */
  public synchronized void connect(RetrospectivePipedInputStream snk) throws IOException {
    if (snk == null) {
      throw new NullPointerException();
    } else if (sink != null || snk.connected) {
      throw new IOException("Already connected");
    }
    sink = snk;
    snk.in = -1;
    snk.out = 0;
    snk.connected = true;
  }

  /**
   * Writes the specified <code>byte</code> to the piped output stream.
   *
   * @param b the <code>byte</code> to be written.
   * @exception IOException if the pipe is broken, unconnected, closed, or if an I/O error occurs.
   */
  public void write(int b) throws IOException {
    if (sink == null) {
      throw new IOException("Pipe not connected");
    }
    sink.receive(b);
  }

  /**
   * Writes <code>len</code> bytes from the specified byte array starting at offset <code>off</code>
   * to this piped output stream. This method blocks until all the bytes are written to the output
   * stream. This uses the optimized bulk receive method for better performance.
   *
   * @param b the data.
   * @param off the start offset in the data.
   * @param len the number of bytes to write.
   * @exception IOException if the pipe is broken, unconnected, closed, or if an I/O error occurs.
   */
  public void write(byte b[], int off, int len) throws IOException {
    if (sink == null) {
      throw new IOException("Pipe not connected");
    } else if (b == null) {
      throw new NullPointerException();
    } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length)
        || ((off + len) < 0)) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {
      return;
    }
    sink.receive(b, off, len);
  }

  /**
   * Flushes this output stream and forces any buffered output bytes to be written out. This will
   * notify any readers that bytes are waiting in the pipe.
   *
   * @exception IOException if an I/O error occurs.
   */
  public synchronized void flush() throws IOException {
    if (sink != null) {
      synchronized (sink) {
        sink.notifyAll();
      }
    }
  }

  /**
   * Closes this piped output stream and releases any system resources associated with this stream.
   * This stream may no longer be used for writing bytes.
   *
   * @exception IOException if an I/O error occurs.
   */
  public void close() throws IOException {
    if (sink != null) {
      sink.receivedLast();
    }
  }
}
