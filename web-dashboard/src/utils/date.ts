const KST_OFFSET = 9 * 60 * 60 * 1000;

/** KST 기준 오늘 날짜 (YYYY-MM-DD) */
export function todayKst(): string {
  return formatKst(new Date());
}

/** KST 기준 N일 전 날짜 (YYYY-MM-DD) */
export function daysAgoKst(n: number): string {
  return formatKst(new Date(Date.now() - n * 86400000));
}

/** Date → KST 기준 YYYY-MM-DD */
function formatKst(d: Date): string {
  const kst = new Date(d.getTime() + KST_OFFSET);
  return kst.toISOString().split("T")[0];
}

/** UTC timestamp → KST 표시 문자열 */
export function formatKstDateTime(utcStr: string): string {
  return new Date(utcStr).toLocaleString("ko-KR", { timeZone: "Asia/Seoul" });
}
