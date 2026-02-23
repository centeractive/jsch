package com.jcraft.jsch;

import java.io.IOException;
import java.io.InputStream;

/**
 * Custom PipedInputStream implementation for better performance. This class is based on the
 * EnhancedPipedInputStream from older Retrospective-JSch versions. It was copied to allow optimized
 * bulk receive operations.
 */
class RetrospectivePipedInputStream extends InputStream {
  boolean closedByWriter = false;
  volatile boolean closedByReader = false;
  boolean connected = false;

  Thread readSide;
  Thread writeSide;

  private static final int DEFAULT_PIPE_SIZE = 1024;

  /**
   * The default size of the pipe's circular input buffer.
   */
  protected static final int PIPE_SIZE = DEFAULT_PIPE_SIZE;

  /**
   * The circular buffer into which incoming data is placed. Initialized lazily to save memory when
   * no data is received.
   */
  protected byte buffer[];

  private int bufferSize;

  /**
   * The index of the position in the circular buffer at which the next byte of data will be stored
   * when received from the connected piped output stream. <code>in&lt;0</code> implies the buffer
   * is empty, <code>in==out</code> implies the buffer is full
   */
  protected int in = -1;

  /**
   * The index of the position in the circular buffer at which the next byte of data will be read by
   * this piped input stream.
   */
  protected int out = 0;

  /**
   * Creates a <code>RetrospectivePipedInputStream</code> so that it is connected to the piped
   * output stream <code>src</code>. Data bytes written to <code>src</code> will then be available
   * as input from this stream.
   *
   * @param src the stream to connect to.
   * @exception IOException if an I/O error occurs.
   */
  public RetrospectivePipedInputStream(RetrospectivePipedOutputStream src) throws IOException {
    this(src, DEFAULT_PIPE_SIZE);
  }

  /**
   * Creates a <code>RetrospectivePipedInputStream</code> so that it is connected to the piped
   * output stream <code>src</code> and uses the specified pipe size for the pipe's buffer. Data
   * bytes written to <code>src</code> will then be available as input from this stream.
   *
   * @param src the stream to connect to.
   * @param pipeSize the size of the pipe's buffer.
   * @exception IOException if an I/O error occurs.
   * @exception IllegalArgumentException if <code>pipeSize <= 0</code>.
   */
  public RetrospectivePipedInputStream(RetrospectivePipedOutputStream src, int pipeSize)
      throws IOException {
    initPipe(pipeSize);
    connect(src);
  }

  /**
   * Creates a <code>RetrospectivePipedInputStream</code> so that it is not yet connected. It must
   * be connected to a <code>RetrospectivePipedOutputStream</code> before being used.
   */
  public RetrospectivePipedInputStream() {
    initPipe(DEFAULT_PIPE_SIZE);
  }

  /**
   * Creates a <code>RetrospectivePipedInputStream</code> so that it is not yet connected and uses
   * the specified pipe size for the pipe's buffer. It must be connected to a
   * <code>RetrospectivePipedOutputStream</code> before being used.
   *
   * @param pipeSize the size of the pipe's buffer.
   * @exception IllegalArgumentException if <code>pipeSize <= 0</code>.
   */
  public RetrospectivePipedInputStream(int pipeSize) {
    initPipe(pipeSize);
  }

  private void initPipe(int pipeSize) {
    if (pipeSize <= 0) {
      throw new IllegalArgumentException("Pipe Size <= 0");
    }
    // Lazy initialization - buffer allocated on first use
    this.bufferSize = pipeSize;
    buffer = null;
  }

  /**
   * Causes this piped input stream to be connected to the piped output stream <code>src</code>. If
   * this object is already connected to some other piped output stream, an <code>IOException</code>
   * is thrown.
   *
   * @param src The piped output stream to connect to.
   * @exception IOException if an I/O error occurs.
   */
  public void connect(RetrospectivePipedOutputStream src) throws IOException {
    src.connect(this);
  }

  /**
   * Receives a byte of data. This method will block if no input is available.
   *
   * @param b the byte being received
   * @exception IOException If the pipe is broken, unconnected, closed, or if an I/O error occurs.
   */
  protected synchronized void receive(int b) throws IOException {
    checkStateForReceive();
    writeSide = Thread.currentThread();
    if (in == out)
      awaitSpace();
    if (in < 0) {
      in = 0;
      out = 0;
    }
    buffer[in++] = (byte) (b & 0xFF);
    if (in >= buffer.length) {
      in = 0;
    }
  }

  /**
   * Receives data into an array of bytes. This method will block until some input is available.
   * This is the key optimization - bulk data transfer using System.arraycopy.
   *
   * @param b the buffer into which the data is received
   * @param off the start offset of the data
   * @param len the maximum number of bytes received
   * @exception IOException If the pipe is broken, unconnected, closed, or if an I/O error occurs.
   */
  synchronized void receive(byte b[], int off, int len) throws IOException {
    checkStateForReceive();
    writeSide = Thread.currentThread();
    if (buffer == null) {
      buffer = new byte[bufferSize];
    }
    int bytesToTransfer = len;
    while (bytesToTransfer > 0) {
      if (in == out)
        awaitSpace();
      int nextTransferAmount = 0;
      if (out < in) {
        nextTransferAmount = buffer.length - in;
      } else if (in < out) {
        if (in == -1) {
          in = out = 0;
          nextTransferAmount = buffer.length - in;
        } else {
          nextTransferAmount = out - in;
        }
      }
      if (nextTransferAmount > bytesToTransfer)
        nextTransferAmount = bytesToTransfer;
      assert (nextTransferAmount > 0);
      System.arraycopy(b, off, buffer, in, nextTransferAmount);
      bytesToTransfer -= nextTransferAmount;
      off += nextTransferAmount;
      in += nextTransferAmount;
      if (in >= buffer.length) {
        in = 0;
      }
    }
  }

  private void checkStateForReceive() throws IOException {
    if (!connected) {
      throw new IOException("Pipe not connected");
    } else if (closedByWriter || closedByReader) {
      throw new IOException("Pipe closed");
    } else if (readSide != null && !readSide.isAlive()) {
      throw new IOException("Read end dead");
    }
  }

  private void awaitSpace() throws IOException {
    while (in == out) {
      checkStateForReceive();

      /* full: kick any waiting readers */
      notifyAll();
      try {
        wait(1000);
      } catch (InterruptedException ex) {
        throw new java.io.InterruptedIOException();
      }
    }
  }

  /**
   * Notifies all waiting threads that the last byte of data has been received.
   */
  synchronized void receivedLast() {
    closedByWriter = true;
    notifyAll();
  }

  /**
   * Reads the next byte of data from this piped input stream. The value byte is returned as an
   * <code>int</code> in the range <code>0</code> to <code>255</code>. This method blocks until
   * input data is available, the end of the stream is detected, or an exception is thrown.
   *
   * @return the next byte of data, or <code>-1</code> if the end of the stream is reached.
   * @exception IOException if the pipe is unconnected, broken, closed, or if an I/O error occurs.
   */
  public synchronized int read() throws IOException {
    if (!connected) {
      throw new IOException("Pipe not connected");
    } else if (closedByReader) {
      throw new IOException("Pipe closed");
    } else if (writeSide != null && !writeSide.isAlive() && !closedByWriter && (in < 0)) {
      throw new IOException("Write end dead");
    }

    readSide = Thread.currentThread();
    int trials = 2;
    while (in < 0) {
      if (closedByWriter) {
        /* closed by writer, return EOF */
        return -1;
      }
      if ((writeSide != null) && (!writeSide.isAlive()) && (--trials < 0)) {
        throw new IOException("Pipe broken");
      }
      /* might be a writer waiting */
      notifyAll();
      try {
        wait(1000);
      } catch (InterruptedException ex) {
        throw new java.io.InterruptedIOException();
      }
    }
    if (buffer == null) {
      buffer = new byte[bufferSize];
    }
    int ret = buffer[out++] & 0xFF;
    if (out >= buffer.length) {
      out = 0;
    }
    if (in == out) {
      /* now empty */
      in = -1;
    }

    return ret;
  }

  /**
   * Reads up to <code>len</code> bytes of data from this piped input stream into an array of bytes.
   * Less than <code>len</code> bytes will be read if the end of the data stream is reached or if
   * <code>len</code> exceeds the pipe's buffer size. If <code>len </code> is zero, then no bytes
   * are read and 0 is returned; otherwise, the method blocks until at least 1 byte of input is
   * available, end of the stream has been detected, or an exception is thrown.
   *
   * @param b the buffer into which the data is read.
   * @param off the start offset in the destination array <code>b</code>
   * @param len the maximum number of bytes read.
   * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no more
   *         data because the end of the stream has been reached.
   * @exception NullPointerException If <code>b</code> is <code>null</code>.
   * @exception IndexOutOfBoundsException If <code>off</code> is negative, <code>len</code> is
   *            negative, or <code>len</code> is greater than <code>b.length - off</code>
   * @exception IOException if the pipe is broken, unconnected, closed, or if an I/O error occurs.
   */
  public synchronized int read(byte b[], int off, int len) throws IOException {
    if (b == null) {
      throw new NullPointerException();
    } else if (off < 0 || len < 0 || len > b.length - off) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {
      return 0;
    }

    /* possibly wait on the first character */
    int c = read();
    if (c < 0) {
      return -1;
    }
    b[off] = (byte) c;
    int rlen = 1;
    if (buffer == null) {
      buffer = new byte[bufferSize];
    }
    while ((in >= 0) && (len > 1)) {

      int available;

      if (in > out) {
        available = Math.min((buffer.length - out), (in - out));
      } else {
        available = buffer.length - out;
      }

      // A byte is read beforehand outside the loop
      if (available > (len - 1)) {
        available = len - 1;
      }
      System.arraycopy(buffer, out, b, off + rlen, available);
      out += available;
      rlen += available;
      len -= available;

      if (out >= buffer.length) {
        out = 0;
      }
      if (in == out) {
        /* now empty */
        in = -1;
      }
    }
    return rlen;
  }

  /**
   * Returns the number of bytes that can be read from this input stream without blocking.
   *
   * @return the number of bytes that can be read from this input stream without blocking, or
   *         {@code 0} if this input stream has been closed by invoking its {@link #close()} method,
   *         or if the pipe is unconnected or broken.
   *
   * @exception IOException if an I/O error occurs.
   */
  public synchronized int available() throws IOException {
    if (in < 0)
      return 0;
    else if (in == out) {
      if (buffer == null) {
        buffer = new byte[bufferSize];
      }
      return buffer.length;
    } else if (in > out)
      return in - out;
    else {
      if (buffer == null) {
        buffer = new byte[bufferSize];
      }
      return in + buffer.length - out;
    }
  }

  /**
   * Closes this piped input stream and releases any system resources associated with the stream.
   *
   * @exception IOException if an I/O error occurs.
   */
  public void close() throws IOException {
    closedByReader = true;
    synchronized (this) {
      in = -1;
    }
  }
}
