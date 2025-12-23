// Modified or written by Object Mentor, Inc. for inclusion with FitNesse.
// Copyright (c) 2002 Cunningham & Cunningham, Inc.
// Released under the terms of the GNU General Public License version 2 or later.
package fit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static util.RegexTestCase.assertSubString;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import fitnesse.socketservice.PlainServerSocketFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.FileUtil;
import util.GradleSupport;
import util.StreamReader;

public class FitServerTest {
  private int portNumber;
  private Process process;
  private Socket socket;
  private ServerSocket serverSocket;
  private InputStream socketInput;
  private OutputStream socketOutput;
  private byte[] httpRequest;
  private ByteArrayOutputStream stdoutBytes;
  private String connectionStatusSize = "0000000000";

  @Before
  public void setup() throws IOException {
    portNumber = 0;
  }

  @After
  public void tearDown() throws Exception {
    if (process != null)
      process.destroy();
    if (socket != null)
      socket.close();
    if (serverSocket != null)
      serverSocket.close();
  }

  @Test
  public void testSimpleStartUp() throws Exception {
    String junkMessage = "x";
    connectionStatusSize = "0000000001";
    prepareSessionProcess();
    assertTrue(new String(httpRequest)
        .startsWith("GET /?responder=socketCatcher&ticket=23"));
    socketOutput.write(junkMessage.getBytes());
    process.waitFor();
  }

  @Test
  public void testBadConnection() throws Exception {
    String errorMessage = "FAILURE";
    connectionStatusSize = "0000000007";
    prepareSessionProcess();
    socketOutput.write(errorMessage.getBytes());

    int exitValue = process.waitFor();
    String stdoutString = new String(stdoutBytes.toByteArray());

    assertTrue(exitValue != 0);
    // TODO This started to fail with Java 5.0... why does -1 turn into 255?
    // assertEquals("stdOut: " + stdoutString, -1, exitValue);
    assertTrue(stdoutString.contains(errorMessage));
  }

  @Test
  public void testNonTestInput() throws Exception {
    prepareSessionProcess();
    socketOutput.write("0000000020".getBytes());
    socketOutput.write("some untestable text".getBytes());
    socketOutput.flush();
    String sizeString = read(10);
    int size = Integer.parseInt(sizeString);
    String output = read(size);
    assertTrue(output.contains("Exception"));
    assertTrue(output.contains("Can't find tag: table"));
  }

  @Test
  public void testOneSimpleRun_Fail() throws Exception {
    String table = simpleTable("FailFixture");
    prepareSessionProcess();
    checkDocumentExecution(table);
    checkDocumentResults(0, 1, 0, 0);
    terminateSessionProcess();

    assertEquals(1, process.exitValue());
  }

  @Test
  public void testOneSimpleRun_Pass() throws Exception {
    String table = simpleTable("PassFixture");
    prepareSessionProcess();
    checkDocumentExecution(table);
    checkDocumentResults(1, 0, 0, 0);
    terminateSessionProcess();

    assertEquals(0, process.exitValue());
  }

  @Test
  public void testTwoSimpleRuns() throws Exception {
    String table = simpleTable("FailFixture");
    prepareSessionProcess();
    checkDocumentExecution(table);
    checkDocumentResults(0, 1, 0, 0);
    checkDocumentExecution(table);
    checkDocumentResults(0, 1, 0, 0);
    terminateSessionProcess();

    assertEquals(2, process.exitValue());
  }

  @Test
  public void testOneMulitiTableRun() throws Exception {
    String document = simpleTable("FailFixture") + simpleTable("FailFixture");
    prepareSessionProcess();

    FitProtocol.writeData(document, socketOutput);

    checkForTwoClassAttributesInResponse();

    checkDocumentResults(0, 2, 0, 0);
    terminateSessionProcess();
    assertEquals(2, process.exitValue());
  }

  @Test
  public void testUnicodeCharacters() throws Exception {
    String table = "\uba80\uba81\uba82\uba83" + simpleTable("PassFixture");
    prepareSessionProcess();
    FitProtocol.writeData(table, socketOutput);
    String response = readWholeResponse();

    assertSubString("\uba80\uba81\uba82\uba83", response);
    terminateSessionProcess();
  }

  @Test
  public void testExtraTextIdPrinted() throws Exception {
    String document = "<html>" + simpleTable("PassFixture") + "monkey"
        + simpleTable("PassFixture") + "</html>";
    prepareSessionProcess();

    FitProtocol.writeData(document, socketOutput);

    String response = readWholeResponse();

    assertTrue(response.startsWith("<html>"));
    assertTrue(response.contains("monkey"));
    assertTrue(response.endsWith("</html>"));
    terminateSessionProcess();
  }

  @Test
  public void testFitParseExceptionDontCrashSuite() throws Exception {
    String input = "no table";
    prepareSessionProcess();
    checkDocumentExecution(input);
    checkDocumentResults(0, 0, 0, 1);
    checkDocumentExecution(simpleTable("PassFixture"));
    checkDocumentResults(1, 0, 0, 0);
    terminateSessionProcess();

    assertEquals(1, process.exitValue());
  }

  private String read(int n) throws Exception {
    return new StreamReader(socketInput).read(n);
  }

  private void prepareSessionProcess() throws Exception {
    serverSocket = new PlainServerSocketFactory().createServerSocket(0);
    serverSocket.setSoTimeout(10000);
    portNumber = serverSocket.getLocalPort();
    checkSentinelToMakeSureThatFitServerIsNotRunning();
    String commandWithArguments = command() + " -s -v localhost " + portNumber
        + " 23";
    process = Runtime.getRuntime().exec(commandWithArguments);

    stdoutBytes = new ByteArrayOutputStream();

    watchForOutput(process.getInputStream(), new PrintStream(stdoutBytes));
    watchForOutput(process.getErrorStream(), System.err);

    establishConnection();
  }

  private void checkSentinelToMakeSureThatFitServerIsNotRunning()
      throws Exception {
    String sentinelName = FitServer.sentinelName(portNumber);
    File sentinel = new File(sentinelName);
    if (sentinel.exists()) {
      FileUtil.deleteFile(sentinelName);
    }
  }

  private void establishConnection() throws Exception {
    socket = acceptClient();

    assertNotNull(socket);
    assertNotNull(socketInput);
    assertNotNull(socketOutput);

    httpRequest = new byte[52]; // the precise length
    socketInput.read(httpRequest);

    socketOutput.write(connectionStatusSize.getBytes());
  }

  private Socket acceptClient() throws Exception {
    try {
      Socket client = serverSocket.accept();
      socketInput = client.getInputStream();
      socketOutput = client.getOutputStream();
      return client;
    } catch (SocketTimeoutException e) {
      String output = stdoutBytes == null ? "" : stdoutBytes.toString();
      throw new AssertionError("Timed out waiting for FitServer to connect. Output: " + output, e);
    }
  }

  private void terminateSessionProcess() throws IOException,
      InterruptedException {
    try {
      socketOutput.write("0000000000".getBytes());
      process.waitFor();
    } finally {
      socketInput.close();
    }
  }

  private void watchForOutput(final InputStream processOutput,
      final PrintStream consoleOutput) {
    new Thread() {
      @Override
      public void run() {
        try {
          int b = 0;
          while ((b = processOutput.read()) != -1)
            consoleOutput.print((char) b);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }.start();
  }

  private void checkDocumentResults(int right, int wrong, int ignored,
      int exceptions) throws Exception {
    Counts actual = FitProtocol.readCounts(new StreamReader(socketInput));

    assertEquals(right, actual.right);
    assertEquals(wrong, actual.wrong);
    assertEquals(ignored, actual.ignores);
    assertEquals(exceptions, actual.exceptions);
  }

  private void checkDocumentExecution(String table) throws Exception {
    FitProtocol.writeData(table, socketOutput);
    checkForAttribute_class();
    checkSize("0000000000");
  }

  private void checkForAttribute_class() throws Exception {
    String output = readFromFitServer();
    assertTrue("'class' attribute was not found",
      output.contains("class="));
  }

  private String readFromFitServer() throws Exception {
    String readSize = read(10);
    int size = Integer.parseInt(readSize);
    String output = read(size);
    return output;
  }

  private void checkSize(String sizeString) throws Exception {
    assertEquals(sizeString, read(10));
  }

  private void checkForTwoClassAttributesInResponse() throws Exception {
    String response = readWholeResponse();
    int first = response.indexOf("class");
    int second = response.indexOf("class", first + 1);
    assertTrue((first >= 0) && (second > first));
  }

  private String readWholeResponse() throws Exception {
    StringBuilder buffer = new StringBuilder();
    String block = readFromFitServer();
    while (!block.isEmpty()) {
      buffer.append(block);
      block = readFromFitServer();
    }
    String response = buffer.toString();
    return response;
  }

  protected String command() {
    return GradleSupport.javaCommand() + " -cp " + GradleSupport.CLASSES_DIR + " fit.FitServer";
  }

  protected String simpleTable(String fixtureName) {
    return "<table>" + "<tr><td>fitnesse.testutil." + fixtureName
        + "</td></tr>" + "</table>";
  }
}
