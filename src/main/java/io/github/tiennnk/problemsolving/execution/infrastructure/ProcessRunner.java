package io.github.tiennnk.problemsolving.execution.infrastructure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.github.tiennnk.problemsolving.execution.application.SandboxException;

class ProcessRunner {

  record Output(int exitCode, String stdout, String stderr, long durationMs, boolean killed, boolean truncated) {
  }

  private record Captured(String text, boolean truncated) {
  }

  Output run(List<String> command, String stdin, Duration timeout, int maxOutputBytes) {
    Process process;
    long start = System.nanoTime();
    try {
      process = new ProcessBuilder(command).start();
    } catch (IOException ex) {
      throw new SandboxException("Cannot start process: " + command.getFirst() + " (is Docker installed and running?)", ex);
    }

    CompletableFuture<Captured> stdout = capture(process.getInputStream(), maxOutputBytes);
    CompletableFuture<Captured> stderr = capture(process.getErrorStream(), maxOutputBytes);
    CompletableFuture.runAsync(() -> writeStdin(process.getOutputStream(), stdin), Thread::startVirtualThread);

    boolean finished;
    try {
      finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException ex) {
      process.destroyForcibly();
      Thread.currentThread().interrupt();
      throw new SandboxException("Interrupted while waiting for process", ex);
    }
    if (!finished) {
      process.destroyForcibly();
    }
    long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

    Captured out = join(stdout);
    Captured err = join(stderr);
    int exitCode = finished ? process.exitValue() : -1;
    return new Output(exitCode, out.text(), err.text(), durationMs, !finished, out.truncated() || err.truncated());
  }

  private static void writeStdin(OutputStream stream, String stdin) {
    try (stream) {
      if (stdin != null) {
        stream.write(stdin.getBytes(StandardCharsets.UTF_8));
      }
    } catch (IOException ignored) {
    }
  }

  private static CompletableFuture<Captured> capture(InputStream stream, int maxBytes) {
    return CompletableFuture.supplyAsync(() -> {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      boolean truncated = false;
      byte[] chunk = new byte[8192];
      try (stream) {
        int read;
        while ((read = stream.read(chunk)) != -1) {
          int room = maxBytes - buffer.size();
          if (room > 0) {
            buffer.write(chunk, 0, Math.min(room, read));
          }
          if (read > room) {
            truncated = true;
          }
        }
      } catch (IOException ignored) {
      }
      return new Captured(buffer.toString(StandardCharsets.UTF_8), truncated);
    }, Thread::startVirtualThread);
  }

  private static Captured join(CompletableFuture<Captured> future) {
    try {
      return future.get(5, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return new Captured("", false);
    } catch (ExecutionException | TimeoutException ex) {
      return new Captured("", false);
    }
  }
}
