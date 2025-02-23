// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.finapp;

import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.grpc.ManagedChannelBuilder;

final class DatabaseModule extends AbstractModule {

  @Override
  protected void configure() {}

  @Provides
  @Singleton
  SpannerDaoInterface provideSpannerDao(
      @ArgsModule.SpannerHost String spannerHost,
      @ArgsModule.SpannerPort int spannerPort,
      @ArgsModule.SpannerProjectId String spannerProjectId,
      @ArgsModule.SpannerInstanceId String spannerInstanceId,
      @ArgsModule.SpannerDatabaseId String spannerDatabaseId,
      @ArgsModule.SpannerUseJdbc boolean spannerUseJdbc) {
    if (spannerUseJdbc) {
      return new SpannerDaoJDBCImpl(spannerProjectId, spannerInstanceId, spannerDatabaseId);
    } else {
      SpannerOptions spannerOptions = SpannerOptions.getDefaultInstance();
      Spanner spanner =
          spannerOptions.toBuilder()
              .setChannelProvider(
                  // Configure GRPC channel explicitly, to simplify deployment on GKE. The default
                  // configuration requires a grpclb to be available.
                  FixedTransportChannelProvider.create(
                      GrpcTransportChannel.create(
                          ManagedChannelBuilder.forAddress(spannerHost, spannerPort).build())))
              .build()
              .getService();
      DatabaseClient client =
          spanner.getDatabaseClient(
              DatabaseId.of(spannerProjectId, spannerInstanceId, spannerDatabaseId));
      return new SpannerDaoImpl(client);
    }
  }
}
