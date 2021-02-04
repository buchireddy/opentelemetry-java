/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.extension.trace.propagation;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.baggage.BaggageEntry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/** Authorization propagator: piggybacking on AwsXrayPropagator integration. */
public final class AwsXrayPropagator implements TextMapPropagator {

  static final String AUTH_HEADER_KEY = "Authorization";
  static final String BAGGAGE_AUTH_KEY = "ZTI-Authorization";

  private static final Logger logger = Logger.getLogger(AwsXrayPropagator.class.getName());

  private static final Collection<String> FIELDS = Collections.singletonList(AUTH_HEADER_KEY);

  private static final AwsXrayPropagator INSTANCE = new AwsXrayPropagator();

  private AwsXrayPropagator() {
    // singleton
  }

  public static AwsXrayPropagator getInstance() {
    return INSTANCE;
  }

  @Override
  public Collection<String> fields() {
    logger.finer("called fields()");
    return FIELDS;
  }

  @Override
  public <C> void inject(Context context, @Nullable C carrier, Setter<C> setter) {
    logger.finer("called inject()");
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(setter, "setter");

    Map<String, BaggageEntry> bag = Baggage.fromContext(context).asMap();
    if (bag.containsKey(BAGGAGE_AUTH_KEY)) {
      String outgoingAuth = bag.get(BAGGAGE_AUTH_KEY).getValue();
      setter.set(carrier, AUTH_HEADER_KEY, outgoingAuth);
      logger.fine("Injected header: " + AUTH_HEADER_KEY);
    }
  }

  @Override
  public <C> Context extract(Context context, @Nullable C carrier, Getter<C> getter) {
    logger.finer("called extract()");
    Objects.requireNonNull(getter, "getter");

    BaggageBuilder builder = Baggage.builder();
    String incomingAuth = getter.get(carrier, AUTH_HEADER_KEY);
    if (incomingAuth != null) {
      builder.put(BAGGAGE_AUTH_KEY, incomingAuth);
      logger.fine("Extracted header: " + AUTH_HEADER_KEY);
    }

    return context.with(builder.build());
  }
}
