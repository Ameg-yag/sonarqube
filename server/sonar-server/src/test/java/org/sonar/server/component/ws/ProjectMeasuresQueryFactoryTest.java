/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.component.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.component.es.ProjectMeasuresQuery;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.component.es.ProjectMeasuresQuery.MetricCriterion;
import static org.sonar.server.component.es.ProjectMeasuresQuery.Operator;
import static org.sonar.server.component.ws.ProjectMeasuresQueryFactory.newProjectMeasuresQuery;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.Level.OK;
import static org.sonar.test.ExceptionCauseMatcher.hasType;

public class ProjectMeasuresQueryFactoryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_query() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery("ncloc > 10 and coverage <= 80");

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(
        tuple("ncloc", Operator.GT, 10d),
        tuple("coverage", Operator.LTE, 80d));
  }

  @Test
  public void create_query_having_equal_operation() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery("ncloc = 10");

    assertThat(query.getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("ncloc", Operator.EQ, 10d));
  }

  @Test
  public void create_query_on_quality_gate() throws Exception {
    ProjectMeasuresQuery query = newProjectMeasuresQuery("alert_status = OK");

    assertThat(query.getQualityGateStatus().name()).isEqualTo(OK.name());
  }

  @Test
  public void fail_to_create_query_on_quality_gate_when_operator_is_not_equal() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    newProjectMeasuresQuery("alert_status > OK");
  }

  @Test
  public void search_is_case_insensitive() throws Exception {
    assertThat(newProjectMeasuresQuery("ncloc > 10 AnD coverage <= 80 AND debt = 10 AND issues = 20").getMetricCriteria()).hasSize(4);
  }

  @Test
  public void convert_metric_to_lower_case() throws Exception {
    assertThat(newProjectMeasuresQuery("NCLOC > 10 AND coVERage <= 80").getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(
        tuple("ncloc", Operator.GT, 10d),
        tuple("coverage", Operator.LTE, 80d));
  }

  @Test
  public void ignore_white_spaces() throws Exception {
    assertThat(newProjectMeasuresQuery("   ncloc    >    10   ").getMetricCriteria())
      .extracting(MetricCriterion::getMetricKey, MetricCriterion::getOperator, MetricCriterion::getValue)
      .containsOnly(tuple("ncloc", Operator.GT, 10d));
  }

  @Test
  public void accept_empty_query() throws Exception {
    ProjectMeasuresQuery result = newProjectMeasuresQuery("");

    assertThat(result.getMetricCriteria()).isEmpty();
  }

  @Test
  public void fail_on_unknown_operator() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectCause(hasType(IllegalArgumentException.class).andMessage("Unknown operator '>='"));
    newProjectMeasuresQuery("ncloc >= 10");
  }

  @Test
  public void fail_on_invalid_criteria() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criterion 'ncloc ? 10'");
    newProjectMeasuresQuery("ncloc ? 10");
  }

  @Test
  public void fail_when_not_double() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criterion 'ncloc > ten'");
    newProjectMeasuresQuery("ncloc > ten");
  }

  @Test
  public void fail_when_no_operator() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criterion 'ncloc 10'");
    newProjectMeasuresQuery("ncloc 10");
  }

  @Test
  public void fail_when_no_key() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criterion '>= 10'");
    newProjectMeasuresQuery(">= 10");
  }

  @Test
  public void fail_when_no_value() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid criterion 'ncloc >='");
    newProjectMeasuresQuery("ncloc >=");
  }

  @Test
  public void private_constructor() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(ProjectMeasuresQueryFactory.class)).isTrue();
  }
}
