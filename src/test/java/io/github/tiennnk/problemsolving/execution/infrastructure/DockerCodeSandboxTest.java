package io.github.tiennnk.problemsolving.execution.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.execution.application.SandboxSession;
import io.github.tiennnk.problemsolving.execution.domain.CompileResult;
import io.github.tiennnk.problemsolving.execution.domain.RunResult;

class DockerCodeSandboxTest {

  private final DockerCodeSandbox sandbox = new DockerCodeSandbox(new SandboxProperties(
      null, null, null, 512, 1.0, 128, Duration.ofSeconds(60), Duration.ofSeconds(3), 0));

  @Test
  void compilesAndRunsJavaAgainstSeveralInputs() {
    String source = """
        import java.util.*;
        public class Main {
          public static void main(String[] args) {
            Scanner in = new Scanner(System.in);
            long a = in.nextLong(), b = in.nextLong();
            System.out.println(a + b);
          }
        }
        """;
    try (SandboxSession session = sandbox.start(Language.JAVA, source)) {
      assertThat(session.compile().success()).isTrue();

      RunResult first = session.run("2 3\n");
      RunResult second = session.run("1000000000 1000000000\n");

      assertThat(first.exitCode()).isZero();
      assertThat(first.stdout().strip()).isEqualTo("5");
      assertThat(second.stdout().strip()).isEqualTo("2000000000");
    }
  }

  @Test
  void reportsCompileErrors() {
    try (SandboxSession session = sandbox.start(Language.JAVA, "public class Main { void broken( }")) {
      CompileResult result = session.compile();

      assertThat(result.success()).isFalse();
      assertThat(result.output()).contains("error");
    }
  }

  @Test
  void killsInfiniteLoops() {
    String source = "public class Main { public static void main(String[] a) { while (true) { } } }";
    try (SandboxSession session = sandbox.start(Language.JAVA, source)) {
      session.compile();

      RunResult result = session.run("");

      assertThat(result.timedOut()).isTrue();
    }
  }

  @Test
  void hasNoNetworkAccess() {
    String source = """
        public class Main {
          public static void main(String[] args) throws Exception {
            new java.net.Socket("1.1.1.1", 80).close();
            System.out.println("connected");
          }
        }
        """;
    try (SandboxSession session = sandbox.start(Language.JAVA, source)) {
      session.compile();

      RunResult result = session.run("");

      assertThat(result.exitCode()).isNotZero();
      assertThat(result.stdout()).doesNotContain("connected");
    }
  }

  @Test
  void runsPhp() {
    String source = """
        <?php
        [$a, $b] = array_map('intval', preg_split('/\\s+/', trim(stream_get_contents(STDIN))));
        echo $a * $b, "\\n";
        """;
    try (SandboxSession session = sandbox.start(Language.PHP, source)) {
      assertThat(session.compile().success()).isTrue();

      assertThat(session.run("6 7\n").stdout().strip()).isEqualTo("42");
    }
  }
}
