import http from "../http";

export function healthApi() {
  return http.get("/health");
}
