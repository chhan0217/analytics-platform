import { initialize, track, trackScreen, identify, resetIdentity, flush } from "@chaevi/analytics-web-sdk";

initialize({
  apiKey: "ink_b6dcfe3468c9e2797f9f132296de67ac3ca06c4ac498e678512073395def220d",
  baseUrl: "http://localhost:8080",
  environment: "development",
  appVersion: "0.1.0",
  flushIntervalMs: 10_000,
  errorListener: (error) => {
    console.error(`[Analytics] ${error.code}: ${error.message}`);
  },
});

console.log("Analytics Web Sample App 시작");

document.addEventListener("DOMContentLoaded", () => {
  const container = document.querySelector(".container");
  if (!container) return;

  const buttons = `
    <div style="margin-top: 2rem; display: flex; flex-direction: column; gap: 0.5rem;">
      <button id="btn-track">커스텀 이벤트 전송</button>
      <button id="btn-screen">화면 조회 이벤트</button>
      <button id="btn-identify">로그인 (identify)</button>
      <button id="btn-reset">로그아웃 (resetIdentity)</button>
      <button id="btn-flush">즉시 전송 (flush)</button>
      <p id="status" style="color: #666;">비로그인 상태</p>
    </div>
  `;
  container.insertAdjacentHTML("beforeend", buttons);

  let eventCount = 0;

  document.getElementById("btn-track")?.addEventListener("click", () => {
    track("product_view", { product_id: `p-${++eventCount}` });
    console.log(`이벤트 전송: product_view #${eventCount}`);
  });

  document.getElementById("btn-screen")?.addEventListener("click", () => {
    trackScreen("sample_page");
    console.log("화면 조회 이벤트 전송");
  });

  document.getElementById("btn-identify")?.addEventListener("click", () => {
    identify("user_1024");
    document.getElementById("status")!.textContent = "로그인 상태: user_1024";
  });

  document.getElementById("btn-reset")?.addEventListener("click", () => {
    resetIdentity();
    document.getElementById("status")!.textContent = "비로그인 상태";
  });

  document.getElementById("btn-flush")?.addEventListener("click", () => {
    flush();
    console.log("즉시 전송 요청");
  });
});
