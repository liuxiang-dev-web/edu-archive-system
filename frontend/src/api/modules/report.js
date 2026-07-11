// 【报表API】归档率、审核统计、指标统计、课程目标/毕业要求达成度。

import http from "../http";

export function archiveRateApi() {
  return http.get("/reports/archive-rate");
}

export function auditStatApi() {
  return http.get("/reports/audit-stat");
}

export function indicatorStatApi() {
  return http.get("/reports/indicator-stat");
}

export function reportArchiveListApi() {
  return http.get("/reports/archive-list");
}

export function courseObjectiveAttainmentApi() {
  return http.get("/reports/course-objective-attainment");
}

export function graduationRequirementAttainmentApi() {
  return http.get("/reports/graduation-requirement-attainment");
}
