import { defineStore } from "pinia";
import type { ServiceItem } from "@/types/domain";

interface OrderState {
  pendingService: ServiceItem | null;
  lastOrderId: number | null;
}

export const useOrderStore = defineStore("order", {
  state: (): OrderState => ({
    pendingService: null,
    lastOrderId: null,
  }),
  actions: {
    setService(service: ServiceItem) {
      this.pendingService = service;
    },
    setLastOrder(orderId: number) {
      this.lastOrderId = orderId;
    },
  },
});
