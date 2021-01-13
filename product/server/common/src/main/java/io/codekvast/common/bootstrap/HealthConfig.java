/*
 * Copyright (c) 2015-2021 Hallin Information Technology AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.common.bootstrap;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthConfig {

  // Use the same name as in
  // org.springframework.boot.actuate.autoconfigure.jdbc.DataSourceHealthContributorAutoConfiguration
  private static final String DB_HEALTH_INDICATOR_BEAN_NAME = "dbHealthIndicator";

  @Bean(DB_HEALTH_INDICATOR_BEAN_NAME)
  public DataSourceHealthIndicator dbHealthIndicator(DataSource dataSource) {
    return new DataSourceHealthIndicator(
        cloneDataSource((HikariDataSource) dataSource), "SELECT 1 FROM DUAL");
  }

  private DataSource cloneDataSource(HikariDataSource dataSource) {
    HikariConfig clone = new HikariConfig();

    dataSource.copyStateTo(clone);
    clone.setMinimumIdle(1);
    clone.setMaximumPoolSize(1);
    clone.setPoolName(dataSource.getPoolName().replace("-1", "-health"));
    return new HikariDataSource(clone);
  }
}
