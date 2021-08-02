/*
 * Copyright 2020 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.finapp;

import com.google.cloud.ByteArray;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.MockSpannerServiceImpl.StatementResult;
import com.google.cloud.spanner.MockSpannerServiceImpl;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.connection.SpannerPool;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FinAppTests {

  private static SpannerDaoInterface JDBCDao;
  private static SpannerDaoInterface JavaDao;
  private static DatabaseClient databaseClient;

  private static MockSpannerServiceImpl mockSpanner;
  private static Server server;
  private static InetSocketAddress address;

  // see: https://github.com/googleapis/java-spanner-jdbc/blob/6d545b617cd2ab21de88a77ce8e9ebe31cf088bf/src/test/java/com/google/cloud/spanner/jdbc/JdbcPreparedStatementWithMockedServerTest.java
  @BeforeClass
  public static void setUpSpanner() throws IOException {
    String spannerProjectId = "test-project";
    String spannerInstanceId = "test-instance";
    String spannerDatabaseId = "test-database";
    // SpannerOptions spannerOptions = SpannerOptions.getDefaultInstance();
    // Spanner spanner = spannerOptions.toBuilder().build().getService();
    // databaseClient = spanner.getDatabaseClient(DatabaseId.of(spannerProjectId, spannerInstanceId, spannerDatabaseId));
    // JavaDao = new SpannerDaoImpl(databaseClient);
    mockSpanner = new MockSpannerServiceImpl();
    mockSpanner.setAbortProbability(0.0D);
    address = new InetSocketAddress("localhost", 0);
    server = NettyServerBuilder.forAddress(address).addService(mockSpanner).build().start();
    JDBCDao = new SpannerDaoJDBCImpl(spannerProjectId, spannerInstanceId, spannerDatabaseId, server.getPort());
  }

  @AfterClass
  public static void stopServer() throws Exception {
    server.shutdown();
    server.awaitTermination();
  }

  @After
  public void reset() {
    SpannerPool.closeSpannerPool();
    mockSpanner.removeAllExecutionTimes();
    mockSpanner.reset();
  }

  @Test
  public void createAccountJDBC() throws SpannerDaoException {
    ByteArray accountId = UuidConverter.getBytesFromUuid(UUID.randomUUID());
    Statement.Builder insertBuilder =
        Statement.newBuilder("INSERT INTO Account\n"
                + "(AccountId, AccountType, AccountStatus, Balance, CreationTimestamp)\n"
                + "VALUES\n"
                + "(?, ?, ?, ?, PENDING_COMMIT_TIMESTAMP())");
    mockSpanner.putStatementResult(
        StatementResult.update(
            insertBuilder.bind("p1").to(accountId).bind("p2").to(
               0).bind("p3").to(0).bind(
                        "p4").to(new BigDecimal(2)).build(), 1L));
    JDBCDao.createAccount(
        accountId,
        AccountType.UNSPECIFIED_ACCOUNT_TYPE,
        AccountStatus.UNSPECIFIED_ACCOUNT_STATUS,
        new BigDecimal(2));
  }
}
