package io.github.tiennnk.problemsolving.execution.infrastructure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.execution.application.CodeSandbox;
import io.github.tiennnk.problemsolving.execution.application.SandboxException;
import io.github.tiennnk.problemsolving.execution.application.SandboxSession;
import io.github.tiennnk.problemsolving.execution.domain.CompileResult;
import io.github.tiennnk.problemsolving.execution.domain.RunResult;

@Component
@EnableConfigurationProperties(SandboxProperties.class)
public class DockerCodeSandbox implements CodeSandbox {

  private static final Logger log = LoggerFactory.getLogger(DockerCodeSandbox.class);
  private static final String WORKDIR = "/sandbox";
  private static final int CONTAINER_MAX_LIFETIME_SECONDS = 600;
  private static final Duration DOCKER_COMMAND_TIMEOUT = Duration.ofSeconds(120);

  private final SandboxProperties properties;
  private final ProcessRunner processRunner = new ProcessRunner();

  public DockerCodeSandbox(SandboxProperties properties) {
    this.properties = properties;
  }

  @Override
  public SandboxSession start(Language language, String sourceCode) {
    if (sourceCode == null || sourceCode.isBlank()) {
      throw new IllegalArgumentException("sourceCode must not be blank");
    }
    LanguageRuntime runtime = LanguageRuntime.of(language, properties);
    String containerName = "ps-sandbox-" + UUID.randomUUID();

    ProcessRunner.Output started = processRunner.run(startCommand(containerName, runtime.image()), null,
        DOCKER_COMMAND_TIMEOUT, properties.maxOutputBytes());
    if (started.exitCode() != 0) {
      throw new SandboxException("Cannot start sandbox container: " + started.stderr().strip());
    }

    DockerSandboxSession session = new DockerSandboxSession(containerName, runtime);
    try {
      session.upload(sourceCode);
    } catch (RuntimeException ex) {
      session.close();
      throw ex;
    }
    return session;
  }

  private List<String> startCommand(String containerName, String image) {
    return List.of(
        properties.dockerCommand(), "run", "--detach", "--rm",
        "--name", containerName,
        "--network", "none",
        "--memory", properties.memoryMb() + "m",
        "--memory-swap", properties.memoryMb() + "m",
        "--cpus", String.valueOf(properties.cpus()),
        "--pids-limit", String.valueOf(properties.pidsLimit()),
        "--read-only",
        "--tmpfs", WORKDIR + ":rw,size=64m,mode=1777",
        "--tmpfs", "/tmp:rw,size=64m,mode=1777",
        "--cap-drop", "ALL",
        "--security-opt", "no-new-privileges",
        "--user", "65534:65534",
        "--workdir", WORKDIR,
        "--entrypoint", "sleep",
        image, String.valueOf(CONTAINER_MAX_LIFETIME_SECONDS));
  }

  private final class DockerSandboxSession implements SandboxSession {

    private final String containerName;
    private final LanguageRuntime runtime;
    private boolean closed;

    private DockerSandboxSession(String containerName, LanguageRuntime runtime) {
      this.containerName = containerName;
      this.runtime = runtime;
    }

    private void upload(String sourceCode) {
      ProcessRunner.Output output = exec(List.of("sh", "-c", "cat > " + WORKDIR + "/" + runtime.fileName()),
          sourceCode, DOCKER_COMMAND_TIMEOUT);
      if (output.exitCode() != 0) {
        throw new SandboxException("Cannot copy source into sandbox: " + output.stderr().strip());
      }
    }

    @Override
    public CompileResult compile() {
      if (runtime.compileCommand().isEmpty()) {
        return CompileResult.skipped();
      }
      ProcessRunner.Output output = exec(runtime.compileCommand(), null, properties.compileTimeout());
      String message = (output.stdout() + output.stderr()).strip();
      if (output.killed()) {
        return new CompileResult(false, "Compilation timed out", output.durationMs());
      }
      return new CompileResult(output.exitCode() == 0, message, output.durationMs());
    }

    @Override
    public RunResult run(String stdin) {
      long limitSeconds = Math.max(1, properties.runTimeout().toSeconds());
      List<String> command = new ArrayList<>(List.of("timeout", "-s", "KILL", String.valueOf(limitSeconds)));
      command.addAll(runtime.runCommand());

      ProcessRunner.Output output = exec(command, stdin, properties.runTimeout().plusSeconds(10));
      boolean timedOut = output.killed() || output.exitCode() == 124
          || (output.exitCode() == 137 && output.durationMs() >= properties.runTimeout().toMillis());
      return new RunResult(output.exitCode(), output.stdout(), output.stderr(), output.durationMs(),
          timedOut, output.truncated());
    }

    @Override
    public void close() {
      if (closed) {
        return;
      }
      closed = true;
      ProcessRunner.Output output = processRunner.run(
          List.of(properties.dockerCommand(), "rm", "--force", containerName), null,
          DOCKER_COMMAND_TIMEOUT, properties.maxOutputBytes());
      if (output.exitCode() != 0) {
        log.warn("Failed to remove sandbox container {}: {}", containerName, output.stderr().strip());
      }
    }

    private ProcessRunner.Output exec(List<String> command, String stdin, Duration timeout) {
      if (closed) {
        throw new SandboxException("Sandbox session is closed");
      }
      List<String> dockerExec = new ArrayList<>(List.of(properties.dockerCommand(), "exec", "--interactive",
          containerName));
      dockerExec.addAll(command);
      return processRunner.run(dockerExec, stdin, timeout, properties.maxOutputBytes());
    }
  }

  record LanguageRuntime(String image, String fileName, List<String> compileCommand, List<String> runCommand) {

    static LanguageRuntime of(Language language, SandboxProperties properties) {
      int heapMb = Math.max(64, properties.memoryMb() / 2);
      return switch (language) {
        case JAVA -> new LanguageRuntime(
            properties.javaImage(),
            "Main.java",
            List.of("javac", "-J-Xmx" + heapMb + "m", "-encoding", "UTF-8", "-d", WORKDIR, WORKDIR + "/Main.java"),
            List.of("java", "-Xmx" + heapMb + "m", "-Xss64m", "-XX:+UseSerialGC", "-XX:-UsePerfData",
                "-Dfile.encoding=UTF-8", "-cp", WORKDIR, "Main"));
        case PHP -> new LanguageRuntime(
            properties.phpImage(),
            "main.php",
            List.of("php", "-l", WORKDIR + "/main.php"),
            List.of("php", "-d", "memory_limit=" + heapMb + "M", WORKDIR + "/main.php"));
      };
    }
  }
}
