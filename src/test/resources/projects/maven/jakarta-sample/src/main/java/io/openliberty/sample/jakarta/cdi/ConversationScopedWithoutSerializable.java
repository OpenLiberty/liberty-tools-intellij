package io.openliberty.sample.jakarta.cdi;

import jakarta.enterprise.context.ConversationScoped;

// Invalid: @ConversationScoped requires Serializable
@ConversationScoped
public class ConversationScopedWithoutSerializable {
}
