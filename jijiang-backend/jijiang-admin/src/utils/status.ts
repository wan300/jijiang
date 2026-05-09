export function serviceStatusText(status?: number) {
  const map: Record<number, string> = {
    0: "待审核",
    1: "已上架",
    2: "已驳回/下架",
  };
  return status === undefined ? "全部" : map[status] || `状态 ${status}`;
}

export function serviceStatusType(status?: number) {
  if (status === 1) return "success";
  if (status === 0) return "warning";
  if (status === 2) return "info";
  return "";
}

export function orderStatusText(status?: number) {
  const map: Record<number, string> = {
    10: "待支付",
    20: "待接单",
    30: "进行中",
    40: "待确认",
    50: "已完成",
    60: "已关闭",
  };
  return status === undefined ? "全部" : map[status] || `状态 ${status}`;
}

export function orderStatusType(status?: number) {
  if (status === 50) return "success";
  if (status === 10 || status === 20 || status === 40) return "warning";
  if (status === 30) return "primary";
  if (status === 60) return "info";
  return "";
}

export function formatMoney(value?: number | string) {
  const numberValue = Number(value || 0);
  return `¥${numberValue.toFixed(2)}`;
}

export function formatDate(value?: string) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 19);
}
