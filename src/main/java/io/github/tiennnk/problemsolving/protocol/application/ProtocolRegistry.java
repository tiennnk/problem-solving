package io.github.tiennnk.problemsolving.protocol.application;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.github.tiennnk.problemsolving.common.domain.ProtocolType;

@Component
public class ProtocolRegistry {

  private final Map<ProtocolType, Protocol> protocols = new EnumMap<>(ProtocolType.class);

  public ProtocolRegistry(List<Protocol> protocols) {
    for (Protocol protocol : protocols) {
      if (this.protocols.put(protocol.type(), protocol) != null) {
        throw new IllegalStateException("Duplicate protocol for " + protocol.type());
      }
    }
  }

  public Protocol get(ProtocolType type) {
    Protocol protocol = protocols.get(type);
    if (protocol == null) {
      throw new IllegalStateException("No protocol registered for " + type);
    }
    return protocol;
  }
}
