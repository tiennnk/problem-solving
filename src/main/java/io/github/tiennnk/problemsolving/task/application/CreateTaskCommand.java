package io.github.tiennnk.problemsolving.task.application;

import io.github.tiennnk.problemsolving.common.domain.Language;
import io.github.tiennnk.problemsolving.common.domain.ProtocolType;

public record CreateTaskCommand(String problem, Language language, ProtocolType protocolType) {
}
